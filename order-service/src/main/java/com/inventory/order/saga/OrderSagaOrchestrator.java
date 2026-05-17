package com.inventory.order.saga;

import com.inventory.common.events.*;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.order.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Choreography-based Saga Orchestrator for the Order fulfillment flow.
 *
 * Saga happy path:
 *   OrderCreated → [Inventory Service reserves stock] → InventoryReserved
 *                → [Payment Service charges customer]  → PaymentProcessed
 *                → OrderConfirmed
 *
 * Compensation paths:
 *   InventoryReservationFailed → OrderCancelled
 *   PaymentFailed → OrderCancelled + InventoryReleased (triggered by inventory service)
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderService orderService;
    private final Counter sagaCompletedCounter;
    private final Counter sagaCompensatedCounter;

    public OrderSagaOrchestrator(OrderService orderService, MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.sagaCompletedCounter = Counter.builder("saga.completed.total")
                .description("Total completed sagas")
                .register(meterRegistry);
        this.sagaCompensatedCounter = Counter.builder("saga.compensated.total")
                .description("Total compensated (failed) sagas")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryReserved(@Payload InventoryReservedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Saga step: InventoryReserved for order: {}, reservationId: {}",
                event.getOrderId(), event.getReservationId());
        orderService.markInventoryReserved(event.getOrderId(), event.getReservationId());
        // Payment service will consume InventoryReservedEvent and process payment
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVATION_FAILED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryReservationFailed(@Payload InventoryReservationFailedEvent event,
                                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("Saga compensation: InventoryReservationFailed for order: {}. Reason: {}",
                event.getOrderId(), event.getReason());
        orderService.cancelOrder(event.getOrderId(),
                "Inventory reservation failed: " + event.getReason());
        sagaCompensatedCounter.increment();
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_PROCESSED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentProcessed(@Payload PaymentProcessedEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Saga step: PaymentProcessed for order: {}, paymentId: {}",
                event.getOrderId(), event.getPaymentId());
        orderService.confirmOrder(event.getOrderId(), event.getPaymentId());
        sagaCompletedCounter.increment();
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(@Payload PaymentFailedEvent event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("Saga compensation: PaymentFailed for order: {}. Reason: {}",
                event.getOrderId(), event.getFailureReason());
        // Cancel the order - inventory service will release reservation when it sees order.cancelled
        orderService.cancelOrder(event.getOrderId(),
                "Payment failed: " + event.getFailureReason());
        sagaCompensatedCounter.increment();
    }
}
