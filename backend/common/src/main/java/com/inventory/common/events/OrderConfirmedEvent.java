package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class OrderConfirmedEvent extends BaseEvent {

    private final UUID orderId;
    private final UUID customerId;
    private final UUID paymentId;
    private final BigDecimal totalAmount;

    @JsonCreator
    public OrderConfirmedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("customerId") UUID customerId,
            @JsonProperty("paymentId") UUID paymentId,
            @JsonProperty("totalAmount") BigDecimal totalAmount) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.paymentId = paymentId;
        this.totalAmount = totalAmount;
    }

    public static OrderConfirmedEvent of(UUID orderId, UUID customerId, UUID paymentId, BigDecimal totalAmount) {
        return new OrderConfirmedEvent(
                java.util.UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), orderId, customerId, paymentId, totalAmount);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getPaymentId() { return paymentId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
