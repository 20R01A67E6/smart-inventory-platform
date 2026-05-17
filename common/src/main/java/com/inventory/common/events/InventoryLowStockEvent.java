package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class InventoryLowStockEvent extends BaseEvent {

    private final UUID productId;
    private final String productName;
    private final int currentStock;
    private final int reorderThreshold;

    @JsonCreator
    public InventoryLowStockEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("productId") UUID productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("currentStock") int currentStock,
            @JsonProperty("reorderThreshold") int reorderThreshold) {
        super(eventId, timestamp, correlationId);
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.reorderThreshold = reorderThreshold;
    }

    public static InventoryLowStockEvent of(UUID productId, String productName,
                                             int currentStock, int reorderThreshold) {
        return new InventoryLowStockEvent(
                UUID.randomUUID().toString(), Instant.now(),
                productId.toString(), productId, productName, currentStock, reorderThreshold);
    }

    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCurrentStock() { return currentStock; }
    public int getReorderThreshold() { return reorderThreshold; }
}
