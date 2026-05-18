package com.inventory.payment.kafka;

import com.inventory.common.events.InventoryReservedEvent;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentService paymentService;

    public PaymentEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryReserved(@Payload InventoryReservedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    Acknowledgment ack) {
        log.info("Received InventoryReservedEvent for order: {}, initiating payment",
                event.getOrderId());
        try {
            // In a real system, the amount and customerId would come from the order service
            // via the event. For this demo, we use the reservationId as a customer proxy.
            BigDecimal amount = new BigDecimal("99.99"); // Would be in the event in production
            UUID customerId = UUID.nameUUIDFromBytes(event.getOrderId().toString().getBytes());

            paymentService.processPayment(event, amount, customerId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to initiate payment for order: {}", event.getOrderId(), e);
            // Circuit breaker/retry will handle - fallback already published failed event
            ack.acknowledge(); // Ack to prevent infinite retry (fallback already fired)
        }
    }
}
