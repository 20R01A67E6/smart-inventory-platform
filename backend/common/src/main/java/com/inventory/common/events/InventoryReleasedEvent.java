package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class InventoryReleasedEvent extends BaseEvent {

    private final UUID orderId;
    private final UUID reservationId;

    @JsonCreator
    public InventoryReleasedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("reservationId") UUID reservationId) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.reservationId = reservationId;
    }

    public static InventoryReleasedEvent of(UUID orderId, UUID reservationId) {
        return new InventoryReleasedEvent(
                UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), orderId, reservationId);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getReservationId() { return reservationId; }
}
