package com.inventory.ml.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restock_alerts")
public class RestockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "recommended_quantity", nullable = false)
    private int recommendedQuantity;

    @Column(name = "days_until_stockout", nullable = false)
    private double daysUntilStockout;

    @Column(name = "avg_daily_demand", nullable = false)
    private double avgDailyDemand;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged = false;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    public RestockAlert() {}

    public static RestockAlert of(UUID productId, String productName, int currentStock,
                                   int recommendedQty, double daysUntilStockout,
                                   double avgDailyDemand, double confidenceScore) {
        RestockAlert a = new RestockAlert();
        a.productId = productId;
        a.productName = productName;
        a.currentStock = currentStock;
        a.recommendedQuantity = recommendedQty;
        a.daysUntilStockout = daysUntilStockout;
        a.avgDailyDemand = avgDailyDemand;
        a.confidenceScore = confidenceScore;
        return a;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCurrentStock() { return currentStock; }
    public int getRecommendedQuantity() { return recommendedQuantity; }
    public double getDaysUntilStockout() { return daysUntilStockout; }
    public double getAvgDailyDemand() { return avgDailyDemand; }
    public double getConfidenceScore() { return confidenceScore; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public Instant getGeneratedAt() { return generatedAt; }
}
