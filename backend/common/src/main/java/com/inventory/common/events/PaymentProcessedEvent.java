package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentProcessedEvent extends BaseEvent {

    private final UUID paymentId;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final String paymentMethod;

    @JsonCreator
    public PaymentProcessedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("paymentId") UUID paymentId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("customerId") UUID customerId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("paymentMethod") String paymentMethod) {
        super(eventId, timestamp, correlationId);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public static PaymentProcessedEvent of(UUID paymentId, UUID orderId, UUID customerId,
                                            BigDecimal amount, String paymentMethod) {
        return new PaymentProcessedEvent(
                UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), paymentId, orderId, customerId, amount, paymentMethod);
    }

    public UUID getPaymentId() { return paymentId; }
    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
}
