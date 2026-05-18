package com.inventory.payment.service;

import com.inventory.common.events.InventoryReservedEvent;
import com.inventory.common.events.PaymentFailedEvent;
import com.inventory.common.events.PaymentProcessedEvent;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.payment.model.Payment;
import com.inventory.payment.model.PaymentStatus;
import com.inventory.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CIRCUIT_BREAKER_NAME = "payment-gateway";
    private static final double SIMULATED_FAILURE_RATE = 0.1; // 10% failure for demo

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter paymentsProcessedCounter;
    private final Counter paymentsFailedCounter;
    private final Counter circuitBreakerFallbackCounter;
    private final Timer paymentProcessingTimer;
    private final Random random = new Random();

    public PaymentService(PaymentRepository paymentRepository,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentsProcessedCounter = Counter.builder("payments.processed.total")
                .description("Total payments processed successfully").register(meterRegistry);
        this.paymentsFailedCounter = Counter.builder("payments.failed.total")
                .description("Total payments failed").register(meterRegistry);
        this.circuitBreakerFallbackCounter = Counter.builder("payments.circuit_breaker.fallback.total")
                .description("Total circuit breaker fallbacks triggered").register(meterRegistry);
        this.paymentProcessingTimer = Timer.builder("payments.processing.duration")
                .description("Payment processing duration").register(meterRegistry);
    }

    /**
     * Processes a payment using the external payment gateway.
     * Protected by Resilience4j Circuit Breaker and Retry.
     *
     * Circuit Breaker: Opens after 5 failures in 10s window, stays open for 30s.
     * Retry: Up to 3 attempts with exponential backoff before circuit breaker records failure.
     */
    @Transactional
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "processPaymentFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public void processPayment(InventoryReservedEvent event, BigDecimal amount, UUID customerId) {
        log.info("Processing payment for order: {}, amount: {}", event.getOrderId(), amount);
        Timer.Sample sample = Timer.start();

        Payment payment = Payment.pending(event.getOrderId(), customerId, amount, "CREDIT_CARD");
        paymentRepository.save(payment);

        try {
            String externalTxId = callExternalPaymentGateway(amount, customerId);
            payment.markProcessed(externalTxId);
            paymentRepository.save(payment);

            PaymentProcessedEvent processed = PaymentProcessedEvent.of(
                    payment.getId(), event.getOrderId(), customerId, amount, "CREDIT_CARD");
            kafkaTemplate.send(KafkaTopics.PAYMENT_PROCESSED, event.getOrderId().toString(), processed);

            paymentsProcessedCounter.increment();
            sample.stop(paymentProcessingTimer);
            log.info("Payment processed: id={}, order={}, txId={}",
                    payment.getId(), event.getOrderId(), externalTxId);

        } catch (Exception e) {
            payment.markFailed(e.getMessage());
            paymentRepository.save(payment);
            sample.stop(paymentProcessingTimer);
            log.warn("Payment failed for order: {}", event.getOrderId(), e);
            throw e; // Rethrow for circuit breaker/retry to handle
        }
    }

    /**
     * Circuit breaker fallback - publishes PaymentFailedEvent when gateway is unavailable.
     */
    public void processPaymentFallback(InventoryReservedEvent event, BigDecimal amount,
                                        UUID customerId, Exception ex) {
        log.error("Circuit breaker OPEN - payment gateway unavailable for order: {}. Cause: {}",
                event.getOrderId(), ex.getMessage());
        circuitBreakerFallbackCounter.increment();
        paymentsFailedCounter.increment();

        Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                .orElse(Payment.pending(event.getOrderId(), customerId, amount, "CREDIT_CARD"));
        payment.markFailed("Payment gateway unavailable: " + ex.getMessage());
        paymentRepository.save(payment);

        PaymentFailedEvent failed = PaymentFailedEvent.of(
                event.getOrderId(), customerId, amount,
                "Payment gateway unavailable: circuit breaker open");
        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, event.getOrderId().toString(), failed);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Payment not found: " + paymentId));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Payment> getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Simulates an external payment gateway call.
     * In production this would call Stripe, PayPal, etc.
     */
    private String callExternalPaymentGateway(BigDecimal amount, UUID customerId) {
        // Simulate network latency (50-200ms)
        try {
            Thread.sleep(50 + random.nextInt(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 10% failure rate
        if (random.nextDouble() < SIMULATED_FAILURE_RATE) {
            throw new RuntimeException("Payment gateway declined: insufficient funds");
        }

        // Return a mock transaction ID
        return "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
