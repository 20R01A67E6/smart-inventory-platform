package com.inventory.order.service;

import com.inventory.order.model.OutboxMessage;
import com.inventory.order.repository.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Transactional Outbox Pattern processor.
 *
 * Polls the outbox_messages table for unprocessed events and publishes them to Kafka.
 * By saving events to the DB in the same transaction as the business entity (Order),
 * we guarantee at-least-once delivery without dual-write consistency issues.
 *
 * Retries up to 5 times before giving up. Failed messages are logged and metrics updated.
 */
@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Timer outboxProcessingTimer;
    private final AtomicLong pendingOutboxCount = new AtomicLong(0);

    public OutboxProcessor(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxProcessingTimer = Timer.builder("outbox.processing.duration")
                .description("Time taken to process outbox messages")
                .register(meterRegistry);
        Gauge.builder("outbox.pending.count", pendingOutboxCount, AtomicLong::get)
                .description("Number of pending outbox messages")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.processor.delay-ms:5000}")
    @Transactional
    public void processOutboxMessages() {
        Timer.Sample sample = Timer.start();
        List<OutboxMessage> pending = outboxRepository.findPendingMessages();

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox messages", pending.size());
        int successCount = 0;
        int failureCount = 0;

        for (OutboxMessage message : pending) {
            try {
                // Synchronous send to ensure message is delivered before marking as processed
                kafkaTemplate.send(message.getTopic(), message.getAggregateId(), message.getPayload())
                        .get();

                message.setProcessed(true);
                message.setProcessedAt(Instant.now());
                outboxRepository.save(message);
                successCount++;

                log.debug("Published outbox message: topic={}, aggregateId={}, type={}",
                        message.getTopic(), message.getAggregateId(), message.getEventType());

            } catch (Exception e) {
                message.incrementRetryCount();
                message.setLastError(truncate(e.getMessage(), 500));
                outboxRepository.save(message);
                failureCount++;

                if (message.getRetryCount() >= MAX_RETRIES) {
                    log.error("Outbox message {} exceeded max retries ({}). Giving up. Topic: {}, AggregateId: {}",
                            message.getId(), MAX_RETRIES, message.getTopic(), message.getAggregateId(), e);
                } else {
                    log.warn("Failed to publish outbox message {} (retry {}/{}): {}",
                            message.getId(), message.getRetryCount(), MAX_RETRIES, e.getMessage());
                }
            }
        }

        long pendingCount = outboxRepository.countPendingMessages();
        pendingOutboxCount.set(pendingCount);
        sample.stop(outboxProcessingTimer);

        if (successCount > 0 || failureCount > 0) {
            log.info("Outbox processing complete: {} published, {} failed, {} still pending",
                    successCount, failureCount, pendingCount);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
