package com.inventory.inventory.kafka;

import com.inventory.common.events.OrderCancelledEvent;
import com.inventory.common.events.OrderCreatedEvent;
import com.inventory.common.events.PaymentFailedEvent;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final InventoryService inventoryService;

    public InventoryEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(@Payload OrderCreatedEvent event,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               Acknowledgment ack) {
        log.info("Received OrderCreatedEvent for order: {}, {} items",
                event.getOrderId(), event.getItems().size());
        try {
            inventoryService.reserveInventory(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent for order: {}", event.getOrderId(), e);
            // Do not ack - message will be redelivered
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCancelled(@Payload OrderCancelledEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment ack) {
        log.info("Received OrderCancelledEvent for order: {}, releasing reservations", event.getOrderId());
        try {
            inventoryService.releaseReservations(event.getOrderId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to release reservations for order: {}", event.getOrderId(), e);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(@Payload PaymentFailedEvent event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                Acknowledgment ack) {
        log.info("Received PaymentFailedEvent for order: {}, releasing reservations", event.getOrderId());
        try {
            inventoryService.releaseReservations(event.getOrderId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to release reservations after payment failure for order: {}",
                    event.getOrderId(), e);
        }
    }
}
