package com.inventory.payment.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.payment.model.Payment;
import com.inventory.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(@PathVariable UUID orderId) {
        Optional<Payment> payment = paymentService.getPaymentByOrderId(orderId);
        return payment.map(p -> ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(p))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No payment found for order: " + orderId)));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    public record PaymentResponse(
            UUID id, UUID orderId, UUID customerId, BigDecimal amount,
            String status, String paymentMethod, String externalTransactionId,
            String failureReason, Instant createdAt) {

        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getCustomerId(),
                    p.getAmount(), p.getStatus().name(), p.getPaymentMethod(),
                    p.getExternalTransactionId(), p.getFailureReason(), p.getCreatedAt());
        }
    }
}
