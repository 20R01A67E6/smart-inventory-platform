package com.inventory.ml.kafka;

import com.inventory.common.events.InventoryLowStockEvent;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.ml.service.DemandForecastingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class LowStockEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LowStockEventConsumer.class);

    private final DemandForecastingService forecastingService;

    public LowStockEventConsumer(DemandForecastingService forecastingService) {
        this.forecastingService = forecastingService;
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_LOW_STOCK,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onLowStockAlert(@Payload InventoryLowStockEvent event, Acknowledgment ack) {
        log.info("Low-stock alert received for product {} ({}), current stock: {}",
                event.getProductName(), event.getProductId(), event.getCurrentStock());
        try {
            DemandForecastingService.ForecastResult result = forecastingService.forecastProduct(
                    event.getProductId(),
                    event.getProductName(),
                    event.getCurrentStock()
            );
            if (result.hasInsufficientData()) {
                log.warn("Insufficient sales history for {} — skipping restock recommendation", event.getProductName());
            } else {
                log.info("Forecast complete for {}: {:.1f} days until stockout, recommend {} units",
                        event.getProductName(), result.daysUntilStockout(), result.recommendedQuantity());
            }
        } catch (Exception ex) {
            log.error("Forecasting failed for product {}: {}", event.getProductId(), ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }
}
