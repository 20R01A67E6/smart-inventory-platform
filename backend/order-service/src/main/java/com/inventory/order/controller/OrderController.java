package com.inventory.order.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.common.dto.OrderRequest;
import com.inventory.order.model.Order;
import com.inventory.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order",
               description = "Creates an order and initiates the order fulfillment saga")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        log.info("POST /api/orders - customerId: {}", request.getCustomerId());
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", OrderResponse.from(order)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order UUID") @PathVariable UUID orderId) {
        log.debug("GET /api/orders/{}", orderId);
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all orders for a customer")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomer(
            @PathVariable UUID customerId) {
        List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId)
                .stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "Customer requested cancellation") String reason) {
        log.info("DELETE /api/orders/{} - reason: {}", orderId, reason);
        orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", null));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Nested response DTO to avoid exposing entity directly
    public record OrderResponse(
            UUID id,
            UUID customerId,
            String status,
            java.math.BigDecimal totalAmount,
            String shippingAddress,
            UUID paymentId,
            UUID reservationId,
            java.time.Instant createdAt,
            List<ItemResponse> items
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.getId(),
                    order.getCustomerId(),
                    order.getStatus().name(),
                    order.getTotalAmount(),
                    order.getShippingAddress(),
                    order.getPaymentId(),
                    order.getReservationId(),
                    order.getCreatedAt(),
                    order.getItems().stream().map(i -> new ItemResponse(
                            i.getId(), i.getProductId(), i.getProductName(),
                            i.getQuantity(), i.getUnitPrice(), i.getSubtotal()
                    )).toList()
            );
        }
    }

    public record ItemResponse(
            UUID id, UUID productId, String productName,
            int quantity, java.math.BigDecimal unitPrice, java.math.BigDecimal subtotal) {}
}
