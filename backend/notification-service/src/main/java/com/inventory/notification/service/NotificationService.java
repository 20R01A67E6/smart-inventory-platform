package com.inventory.notification.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_LOG_HISTORY = 1000;

    private final CopyOnWriteArrayList<NotificationRecord> recentNotifications = new CopyOnWriteArrayList<>();
    private final Counter notificationsCounter;

    public NotificationService(MeterRegistry meterRegistry) {
        this.notificationsCounter = Counter.builder("notifications.total")
                .description("Total notifications processed")
                .register(meterRegistry);
    }

    public void notify(String eventType, String message) {
        log.info("[NOTIFICATION] [{}] {}", eventType, message);
        notificationsCounter.increment();

        recentNotifications.add(new NotificationRecord(eventType, message, Instant.now()));

        // Trim to prevent unbounded growth
        while (recentNotifications.size() > MAX_LOG_HISTORY) {
            recentNotifications.remove(0);
        }
    }

    public List<NotificationRecord> getRecentNotifications(int limit) {
        List<NotificationRecord> all = new ArrayList<>(recentNotifications);
        Collections.reverse(all);
        return all.stream().limit(limit).toList();
    }

    public record NotificationRecord(String eventType, String message, Instant timestamp) {}
}
