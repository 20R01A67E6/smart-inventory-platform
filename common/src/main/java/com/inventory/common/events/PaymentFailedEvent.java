package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentFailedEvent extends BaseEvent {

    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final String failureReason;

    @JsonCreator
    public PaymentFailedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("customerId") UUID customerId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("failureReason") String failureReason) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.failureReason = failureReason;
    }

    public static PaymentFailedEvent of(UUID orderId, UUID customerId, BigDecimal amount, String reason) {
        return new PaymentFailedEvent(
                UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), orderId, customerId, amount, reason);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getFailureReason() { return failureReason; }
}
