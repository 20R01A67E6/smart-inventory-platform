package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class InventoryReservationFailedEvent extends BaseEvent {

    private final UUID orderId;
    private final String reason;
    private final UUID productId;

    @JsonCreator
    public InventoryReservationFailedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("reason") String reason,
            @JsonProperty("productId") UUID productId) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.reason = reason;
        this.productId = productId;
    }

    public static InventoryReservationFailedEvent of(UUID orderId, UUID productId, String reason) {
        return new InventoryReservationFailedEvent(
                UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), orderId, reason, productId);
    }

    public UUID getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public UUID getProductId() { return productId; }
}
