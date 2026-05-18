package com.inventory.order.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSING,
    CONFIRMED,
    CANCELLED,
    COMPENSATION_FAILED
}
