package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class InventoryReservedEvent extends BaseEvent {

    private final UUID orderId;
    private final UUID reservationId;
    private final List<ReservedItem> reservedItems;

    @JsonCreator
    public InventoryReservedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("reservationId") UUID reservationId,
            @JsonProperty("reservedItems") List<ReservedItem> reservedItems) {
        super(eventId, timestamp, correlationId);
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.reservedItems = reservedItems;
    }

    public static InventoryReservedEvent of(UUID orderId, UUID reservationId, List<ReservedItem> items) {
        return new InventoryReservedEvent(
                UUID.randomUUID().toString(), Instant.now(),
                orderId.toString(), orderId, reservationId, items);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getReservationId() { return reservationId; }
    public List<ReservedItem> getReservedItems() { return reservedItems; }

    public record ReservedItem(
            @JsonProperty("productId") UUID productId,
            @JsonProperty("quantity") int quantity) {}
}
