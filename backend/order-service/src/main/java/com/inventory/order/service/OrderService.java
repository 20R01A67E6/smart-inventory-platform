package com.inventory.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.dto.OrderItemRequest;
import com.inventory.common.dto.OrderRequest;
import com.inventory.common.events.OrderCancelledEvent;
import com.inventory.common.events.OrderConfirmedEvent;
import com.inventory.common.events.OrderCreatedEvent;
import com.inventory.common.events.OrderCreatedEvent.OrderItemEvent;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.order.model.Order;
import com.inventory.order.model.OrderItem;
import com.inventory.order.model.OrderStatus;
import com.inventory.order.model.OutboxMessage;
import com.inventory.order.repository.OrderRepository;
import com.inventory.order.repository.OutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Counter ordersCreatedCounter;
    private final Counter ordersCancelledCounter;
    private final Counter ordersConfirmedCounter;

    // Injected from inventory-service via REST in a real system; here we use a stub map.
    // Must stay in sync with inventory-service Flyway seed data (V1 + V2 migrations).
    private static final Map<UUID, ProductInfo> PRODUCT_CATALOG = Map.of(
            // V1 seed products
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    new ProductInfo("Laptop Pro 15", new BigDecimal("1299.99")),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    new ProductInfo("Wireless Mouse", new BigDecimal("29.99")),
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    new ProductInfo("USB-C Hub", new BigDecimal("49.99")),
            UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    new ProductInfo("Standing Desk Mat", new BigDecimal("79.99")),
            UUID.fromString("55555555-5555-5555-5555-555555555555"),
                    new ProductInfo("Mechanical Keyboard", new BigDecimal("149.99")),
            // V2 seed products
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                    new ProductInfo("Test Widget Alpha", new BigDecimal("9.99")),
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
                    new ProductInfo("Test Widget Beta", new BigDecimal("19.99")),
            UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012"),
                    new ProductInfo("Test Gadget Gamma", new BigDecimal("49.99")),
            UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123"),
                    new ProductInfo("Test Accessory Delta", new BigDecimal("4.99"))
    );

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
                        ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .register(meterRegistry);
        this.ordersCancelledCounter = Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .register(meterRegistry);
        this.ordersConfirmedCounter = Counter.builder("orders.confirmed.total")
                .description("Total number of orders confirmed")
                .register(meterRegistry);
    }

    /**
     * Creates an order and saves an outbox event atomically in the same transaction.
     * The OutboxProcessor will pick up the event and publish it to Kafka asynchronously.
     */
    @Transactional
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        // Build order with items
        Order order = Order.create(request.getCustomerId(), BigDecimal.ZERO, request.getShippingAddress());
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            ProductInfo product = PRODUCT_CATALOG.getOrDefault(
                    itemReq.getProductId(),
                    new ProductInfo("Unknown Product", new BigDecimal("9.99"))
            );
            OrderItem item = OrderItem.of(
                    itemReq.getProductId(),
                    product.name(),
                    itemReq.getQuantity(),
                    product.price()
            );
            order.addItem(item);
            total = total.add(product.price().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        // Simulate setting total (in real system we'd call inventory service for price)
        var savedOrder = orderRepository.save(order);

        // Build and save outbox event in the SAME transaction
        List<OrderItemEvent> eventItems = savedOrder.getItems().stream()
                .map(i -> new OrderItemEvent(i.getProductId(), i.getProductName(),
                        i.getQuantity(), i.getUnitPrice()))
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.of(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                eventItems,
                total,
                savedOrder.getShippingAddress()
        );

        saveOutboxMessage(savedOrder.getId().toString(), event, KafkaTopics.ORDER_CREATED);

        ordersCreatedCounter.increment();
        log.info("Order created with ID: {}, outbox event saved", savedOrder.getId());
        return savedOrder;
    }

    @Transactional
    public void confirmOrder(UUID orderId, UUID paymentId) {
        Order order = findOrderOrThrow(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentId(paymentId);
        orderRepository.save(order);

        OrderConfirmedEvent event = OrderConfirmedEvent.of(
                orderId, order.getCustomerId(), paymentId, order.getTotalAmount());
        saveOutboxMessage(orderId.toString(), event, KafkaTopics.ORDER_CONFIRMED);

        ordersConfirmedCounter.increment();
        log.info("Order {} confirmed with payment {}", orderId, paymentId);
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.warn("Attempted to cancel already-confirmed order: {}", orderId);
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        orderRepository.save(order);

        OrderCancelledEvent event = OrderCancelledEvent.of(
                orderId, order.getCustomerId(), reason);
        saveOutboxMessage(orderId.toString(), event, KafkaTopics.ORDER_CANCELLED);

        ordersCancelledCounter.increment();
        log.info("Order {} cancelled. Reason: {}", orderId, reason);
    }

    @Transactional
    public void markInventoryReserved(UUID orderId, UUID reservationId) {
        Order order = findOrderOrThrow(orderId);
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        order.setReservationId(reservationId);
        orderRepository.save(order);
        log.info("Order {} inventory reserved, reservationId: {}", orderId, reservationId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdWithItems(customerId);
    }

    private void saveOutboxMessage(String aggregateId, Object event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxMessage outbox = OutboxMessage.of(
                    aggregateId,
                    event.getClass().getSimpleName(),
                    topic,
                    payload
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event for outbox: " + event.getClass().getSimpleName(), e);
        }
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    private record ProductInfo(String name, BigDecimal price) {}
}
