package com.inventory.notification.kafka;

import com.inventory.common.events.*;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCreated(@Payload OrderCreatedEvent event, Acknowledgment ack) {
        notificationService.notify("ORDER_CREATED",
                String.format("New order %s from customer %s for $%s",
                        event.getOrderId(), event.getCustomerId(), event.getTotalAmount()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onOrderConfirmed(@Payload OrderConfirmedEvent event, Acknowledgment ack) {
        notificationService.notify("ORDER_CONFIRMED",
                String.format("Order %s confirmed. Payment: %s", event.getOrderId(), event.getPaymentId()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCancelled(@Payload OrderCancelledEvent event, Acknowledgment ack) {
        notificationService.notify("ORDER_CANCELLED",
                String.format("Order %s cancelled. Reason: %s", event.getOrderId(), event.getReason()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryReserved(@Payload InventoryReservedEvent event, Acknowledgment ack) {
        notificationService.notify("INVENTORY_RESERVED",
                String.format("Inventory reserved for order %s (%d items)",
                        event.getOrderId(), event.getReservedItems().size()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVATION_FAILED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryReservationFailed(@Payload InventoryReservationFailedEvent event, Acknowledgment ack) {
        notificationService.notify("INVENTORY_RESERVATION_FAILED",
                String.format("[ALERT] Inventory reservation FAILED for order %s: %s",
                        event.getOrderId(), event.getReason()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_PROCESSED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentProcessed(@Payload PaymentProcessedEvent event, Acknowledgment ack) {
        notificationService.notify("PAYMENT_PROCESSED",
                String.format("Payment %s processed for order %s. Amount: $%s via %s",
                        event.getPaymentId(), event.getOrderId(),
                        event.getAmount(), event.getPaymentMethod()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentFailed(@Payload PaymentFailedEvent event, Acknowledgment ack) {
        notificationService.notify("PAYMENT_FAILED",
                String.format("[ALERT] Payment FAILED for order %s. Reason: %s",
                        event.getOrderId(), event.getFailureReason()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_LOW_STOCK,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onLowStock(@Payload InventoryLowStockEvent event, Acknowledgment ack) {
        notificationService.notify("LOW_STOCK_ALERT",
                String.format("[ALERT] Low stock: %s (id=%s) has %d units (threshold: %d)",
                        event.getProductName(), event.getProductId(),
                        event.getCurrentStock(), event.getReorderThreshold()));
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaTopics.ML_RESTOCK_RECOMMENDED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onRestockRecommended(@Payload RestockRecommendedEvent event, Acknowledgment ack) {
        notificationService.notify("RESTOCK_RECOMMENDED",
                String.format("[ML] Restock %s: recommend ordering %d units. Days until stockout: %.1f (confidence: %.0f%%)",
                        event.getProductName(), event.getRecommendedQuantity(),
                        event.getDaysUntilStockout(), event.getConfidenceScore() * 100));
        ack.acknowledge();
    }
}
