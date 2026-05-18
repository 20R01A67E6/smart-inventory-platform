package com.inventory.inventory.service;

import com.inventory.common.dto.ProductRequest;
import com.inventory.common.events.*;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.inventory.model.InventoryReservation;
import com.inventory.inventory.model.Product;
import com.inventory.inventory.model.ReservationStatus;
import com.inventory.inventory.repository.InventoryReservationRepository;
import com.inventory.inventory.repository.ProductRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String PRODUCTS_CACHE = "products";
    private static final String PRODUCT_CACHE = "product";

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public InventoryService(ProductRepository productRepository,
                            InventoryReservationRepository reservationRepository,
                            KafkaTemplate<String, Object> kafkaTemplate,
                            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;

        // Register gauge for total product count
        Gauge.builder("inventory.products.total", productRepository, r -> r.count())
                .description("Total number of products")
                .register(meterRegistry);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getSku());
        Product product = new Product();
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setReorderThreshold(request.getReorderThreshold());
        product.setCategory(request.getCategory());
        return productRepository.save(product);
    }

    @Cacheable(value = PRODUCT_CACHE, key = "#productId")
    @Transactional(readOnly = true)
    public Product getProduct(UUID productId) {
        log.debug("Cache miss for product: {}", productId);
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
    }

    @Cacheable(value = PRODUCTS_CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.debug("Cache miss for all products");
        return productRepository.findByActiveTrue();
    }

    @CacheEvict(value = {PRODUCT_CACHE, PRODUCTS_CACHE}, allEntries = true)
    @Transactional
    public Product updateStock(UUID productId, int newQuantity) {
        log.info("Updating stock for product {}: {}", productId, newQuantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        product.setStockQuantity(newQuantity);
        Product saved = productRepository.save(product);

        // Register per-product stock gauge
        Gauge.builder("inventory.stock.level", saved, p -> p.getStockQuantity())
                .description("Current stock level")
                .tag("product_id", productId.toString())
                .tag("product_name", saved.getName())
                .register(meterRegistry);

        return saved;
    }

    @CacheEvict(value = {PRODUCT_CACHE, PRODUCTS_CACHE}, key = "#productId")
    @Transactional
    public Product updateProduct(UUID productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setReorderThreshold(request.getReorderThreshold());
        product.setCategory(request.getCategory());
        return productRepository.save(product);
    }

    /**
     * Reserves inventory for an order atomically using pessimistic locking.
     * Publishes InventoryReservedEvent or InventoryReservationFailedEvent to Kafka.
     */
    @Transactional
    @CacheEvict(value = PRODUCT_CACHE, allEntries = true)
    public void reserveInventory(OrderCreatedEvent event) {
        log.info("Processing inventory reservation for order: {}", event.getOrderId());
        List<InventoryReservedEvent.ReservedItem> reservedItems = new ArrayList<>();
        UUID firstFailedProductId = null;
        String failureReason = null;

        try {
            for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
                // Pessimistic lock to prevent concurrent reservation race conditions
                Product product = productRepository.findByIdWithLock(item.productId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Product not found: " + item.productId()));

                if (!product.hasEnoughStock(item.quantity())) {
                    firstFailedProductId = item.productId();
                    failureReason = String.format(
                            "Insufficient stock for %s: requested=%d, available=%d",
                            product.getName(), item.quantity(), product.getStockQuantity());
                    throw new IllegalStateException(failureReason);
                }

                product.reserveStock(item.quantity());
                productRepository.save(product);

                InventoryReservation reservation = InventoryReservation.create(
                        event.getOrderId(), item.productId(), item.quantity());
                reservationRepository.save(reservation);
                reservedItems.add(new InventoryReservedEvent.ReservedItem(
                        item.productId(), item.quantity()));

                // Check and alert for low stock
                if (product.isBelowReorderThreshold()) {
                    publishLowStockAlert(product);
                }
            }

            // All items reserved successfully
            UUID reservationGroupId = reservedItems.isEmpty()
                    ? UUID.randomUUID()
                    : reservationRepository.findByOrderId(event.getOrderId())
                      .stream().findFirst().map(r -> r.getId()).orElse(UUID.randomUUID());

            InventoryReservedEvent reserved = InventoryReservedEvent.of(
                    event.getOrderId(), reservationGroupId, reservedItems);
            kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVED, event.getOrderId().toString(), reserved);
            log.info("Inventory reserved for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.warn("Inventory reservation failed for order {}: {}", event.getOrderId(), e.getMessage());
            InventoryReservationFailedEvent failed = InventoryReservationFailedEvent.of(
                    event.getOrderId(),
                    firstFailedProductId,
                    e.getMessage()
            );
            // Publish in a nested try so a transient broker error does not prevent the
            // Kafka consumer from acknowledging — without this, the broker hiccup would
            // cause onOrderCreated to skip ack.acknowledge(), triggering infinite redelivery.
            try {
                kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVATION_FAILED,
                        event.getOrderId().toString(), failed);
            } catch (Exception kafkaEx) {
                log.error("Failed to publish InventoryReservationFailedEvent for order {}: {}",
                        event.getOrderId(), kafkaEx.getMessage(), kafkaEx);
            }
        }
    }

    /**
     * Releases all active reservations for an order (saga compensation step).
     */
    @Transactional
    @CacheEvict(value = PRODUCT_CACHE, allEntries = true)
    public void releaseReservations(UUID orderId) {
        log.info("Releasing inventory reservations for order: {}", orderId);
        List<InventoryReservation> activeReservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);

        for (InventoryReservation reservation : activeReservations) {
            Product product = productRepository.findByIdWithLock(reservation.getProductId())
                    .orElseGet(() -> {
                        log.error("Product not found during reservation release: {}", reservation.getProductId());
                        return null;
                    });

            if (product != null) {
                product.releaseStock(reservation.getQuantity());
                productRepository.save(product);
            }
            reservation.release();
            reservationRepository.save(reservation);
        }

        if (!activeReservations.isEmpty()) {
            InventoryReleasedEvent released = InventoryReleasedEvent.of(
                    orderId,
                    activeReservations.get(0).getId()
            );
            kafkaTemplate.send(KafkaTopics.INVENTORY_RELEASED, orderId.toString(), released);
            log.info("Released {} reservations for order: {}", activeReservations.size(), orderId);
        }
    }

    private void publishLowStockAlert(Product product) {
        log.warn("Low stock alert: product={}, stock={}, threshold={}",
                product.getName(), product.getStockQuantity(), product.getReorderThreshold());
        InventoryLowStockEvent event = InventoryLowStockEvent.of(
                product.getId(),
                product.getName(),
                product.getStockQuantity(),
                product.getReorderThreshold()
        );
        kafkaTemplate.send(KafkaTopics.INVENTORY_LOW_STOCK, product.getId().toString(), event);
    }
}
