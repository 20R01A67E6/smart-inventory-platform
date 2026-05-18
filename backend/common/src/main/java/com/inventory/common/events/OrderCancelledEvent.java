package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class OrderCancelledEvent extends BaseEvent {

    private final UUID orderId;
    private final UUID customerId;
    private final String reason;

    @JsonCreator
    public OrderCancelledEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("customerId") UUID customerId,
            @JsonProperty("reason") String reason) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.reason = reason;
    }

    public static OrderCancelledEvent of(UUID orderId, UUID customerId, String reason) {
        return new OrderCancelledEvent(
                UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), orderId, customerId, reason);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public String getReason() { return reason; }
}
