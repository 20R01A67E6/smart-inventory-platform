package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class RestockRecommendedEvent extends BaseEvent {

    private final UUID productId;
    private final String productName;
    private final int currentStock;
    private final int recommendedQuantity;
    private final double daysUntilStockout;
    private final double confidenceScore;

    @JsonCreator
    public RestockRecommendedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("productId") UUID productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("currentStock") int currentStock,
            @JsonProperty("recommendedQuantity") int recommendedQuantity,
            @JsonProperty("daysUntilStockout") double daysUntilStockout,
            @JsonProperty("confidenceScore") double confidenceScore) {
        super(eventId, timestamp, correlationId);
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.recommendedQuantity = recommendedQuantity;
        this.daysUntilStockout = daysUntilStockout;
        this.confidenceScore = confidenceScore;
    }

    public static RestockRecommendedEvent of(UUID productId, String productName, int currentStock,
                                              int recommendedQty, double daysUntilStockout,
                                              double confidenceScore) {
        return new RestockRecommendedEvent(
                UUID.randomUUID().toString(), Instant.now(),
                productId.toString(), productId, productName, currentStock,
                recommendedQty, daysUntilStockout, confidenceScore);
    }

    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCurrentStock() { return currentStock; }
    public int getRecommendedQuantity() { return recommendedQuantity; }
    public double getDaysUntilStockout() { return daysUntilStockout; }
    public double getConfidenceScore() { return confidenceScore; }
}
