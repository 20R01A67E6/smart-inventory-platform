package com.inventory.ml.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_history", indexes = {
        @Index(name = "idx_sales_product_date", columnList = "product_id, sale_date"),
        @Index(name = "idx_sales_date", columnList = "sale_date")
})
public class SalesHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "quantity_sold", nullable = false)
    private int quantitySold;

    @Column(name = "order_id")
    private UUID orderId;

    public SalesHistory() {}

    public static SalesHistory of(UUID productId, String productName, LocalDate date,
                                   int quantity, UUID orderId) {
        SalesHistory s = new SalesHistory();
        s.productId = productId;
        s.productName = productName;
        s.saleDate = date;
        s.quantitySold = quantity;
        s.orderId = orderId;
        return s;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public LocalDate getSaleDate() { return saleDate; }
    public int getQuantitySold() { return quantitySold; }
    public UUID getOrderId() { return orderId; }
}
