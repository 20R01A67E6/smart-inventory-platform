package com.inventory.inventory.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.common.dto.ProductRequest;
import com.inventory.inventory.model.Product;
import com.inventory.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Inventory", description = "Product inventory management endpoints")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/products")
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        Product product = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", ProductResponse.from(product)));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get product by ID (cached)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID productId) {
        Product product = inventoryService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(ProductResponse.from(product)));
    }

    @GetMapping("/products")
    @Operation(summary = "Get all active products (cached)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> products = inventoryService.getAllProducts()
                .stream().map(ProductResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping("/products/{productId}")
    @Operation(summary = "Update product details (invalidates cache)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request) {
        Product product = inventoryService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", ProductResponse.from(product)));
    }

    @PatchMapping("/inventory/{productId}/stock")
    @Operation(summary = "Update stock level (invalidates cache)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        if (quantity < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Stock quantity cannot be negative"));
        }
        Product product = inventoryService.updateStock(productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated", ProductResponse.from(product)));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    public record ProductResponse(
            UUID id, String name, String sku, String description,
            BigDecimal price, int stockQuantity, int reorderThreshold,
            String category, boolean active, Instant createdAt) {

        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getSku(), p.getDescription(),
                    p.getPrice(), p.getStockQuantity(), p.getReorderThreshold(),
                    p.getCategory(), p.isActive(), p.getCreatedAt());
        }
    }
}
