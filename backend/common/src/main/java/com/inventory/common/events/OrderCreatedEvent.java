package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent extends BaseEvent {

    private final UUID orderId;
    private final UUID customerId;
    private final List<OrderItemEvent> items;
    private final BigDecimal totalAmount;
    private final String shippingAddress;

    @JsonCreator
    public OrderCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("customerId") UUID customerId,
            @JsonProperty("items") List<OrderItemEvent> items,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("shippingAddress") String shippingAddress) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }

    public static OrderCreatedEvent of(UUID orderId, UUID customerId,
                                       List<OrderItemEvent> items, BigDecimal totalAmount,
                                       String shippingAddress) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                orderId.toString(),
                orderId, customerId, items, totalAmount, shippingAddress);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public List<OrderItemEvent> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getShippingAddress() { return shippingAddress; }

    public record OrderItemEvent(
            @JsonProperty("productId") UUID productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") BigDecimal unitPrice) {}
}
