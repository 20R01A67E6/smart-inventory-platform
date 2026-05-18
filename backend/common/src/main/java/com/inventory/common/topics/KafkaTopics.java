package com.inventory.common.topics;

public final class KafkaTopics {

    private KafkaTopics() {}

    // Order topics
    public static final String ORDER_CREATED           = "order.created";
    public static final String ORDER_CONFIRMED         = "order.confirmed";
    public static final String ORDER_CANCELLED         = "order.cancelled";

    // Inventory topics
    public static final String INVENTORY_RESERVED         = "inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory.reservation.failed";
    public static final String INVENTORY_RELEASED         = "inventory.released";
    public static final String INVENTORY_LOW_STOCK        = "inventory.low-stock";

    // Payment topics
    public static final String PAYMENT_PROCESSED = "payment.processed";
    public static final String PAYMENT_FAILED    = "payment.failed";

    // ML topics
    public static final String ML_RESTOCK_RECOMMENDED = "ml.restock.recommended";
}
