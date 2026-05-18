package com.inventory.ml.service;

import com.inventory.common.events.RestockRecommendedEvent;
import com.inventory.common.topics.KafkaTopics;
import com.inventory.ml.model.RestockAlert;
import com.inventory.ml.model.SalesHistory;
import com.inventory.ml.repository.RestockAlertRepository;
import com.inventory.ml.repository.SalesHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DemandForecastingService {

    private static final Logger log = LoggerFactory.getLogger(DemandForecastingService.class);

    private static final int FORECAST_WINDOW_DAYS = 30;
    private static final double STOCKOUT_ALERT_THRESHOLD_DAYS = 14.0;
    private static final double SAFETY_STOCK_MULTIPLIER = 1.5;

    // Weighted moving average weights (most recent days get higher weight)
    // 7-day WMA: day-7 has weight 1, day-6 has weight 2, ..., day-1 has weight 7
    private static final int WMA_PERIOD = 7;

    private final SalesHistoryRepository salesHistoryRepository;
    private final RestockAlertRepository restockAlertRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DemandForecastingService(SalesHistoryRepository salesHistoryRepository,
                                     RestockAlertRepository restockAlertRepository,
                                     KafkaTemplate<String, Object> kafkaTemplate) {
        this.salesHistoryRepository = salesHistoryRepository;
        this.restockAlertRepository = restockAlertRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Runs demand forecasting for a product using Weighted Moving Average.
     * Generates a restock alert if days-until-stockout < threshold.
     *
     * WMA formula: WMA = (d1*1 + d2*2 + ... + dn*n) / (1+2+...+n)
     * where d_n is the most recent day's demand.
     */
    @Transactional
    public ForecastResult forecastProduct(UUID productId, String productName, int currentStock) {
        LocalDate since = LocalDate.now().minusDays(FORECAST_WINDOW_DAYS);
        List<SalesHistory> history = salesHistoryRepository.findByProductIdSince(productId, since);

        if (history.isEmpty()) {
            log.debug("No sales history for product: {}", productId);
            return ForecastResult.noData(productId, productName, currentStock);
        }

        // Aggregate by date
        Map<LocalDate, Integer> dailySales = history.stream()
                .collect(Collectors.groupingBy(
                        SalesHistory::getSaleDate,
                        Collectors.summingInt(SalesHistory::getQuantitySold)
                ));

        // Fill missing days with 0
        List<Integer> salesByDay = new ArrayList<>();
        for (int i = FORECAST_WINDOW_DAYS - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            salesByDay.add(dailySales.getOrDefault(date, 0));
        }

        double wmaDaily = computeWeightedMovingAverage(salesByDay);
        double simpleAvgDaily = salesByDay.stream().mapToInt(Integer::intValue).average().orElse(0);

        // Confidence based on data consistency
        double stdDev = computeStdDev(salesByDay, simpleAvgDaily);
        double coefficientOfVariation = simpleAvgDaily > 0 ? stdDev / simpleAvgDaily : 1.0;
        double confidenceScore = Math.max(0.0, Math.min(1.0, 1.0 - coefficientOfVariation));

        double daysUntilStockout = wmaDaily > 0 ? currentStock / wmaDaily : Double.MAX_VALUE;
        int recommendedQuantity = wmaDaily > 0
                ? (int) Math.ceil(wmaDaily * 30 * SAFETY_STOCK_MULTIPLIER) // 30-day supply + safety stock
                : 0;

        ForecastResult result = new ForecastResult(
                productId, productName, currentStock, wmaDaily,
                daysUntilStockout, recommendedQuantity, confidenceScore
        );

        // Generate alert if close to stockout
        if (daysUntilStockout < STOCKOUT_ALERT_THRESHOLD_DAYS && recommendedQuantity > 0) {
            generateRestockAlert(result);
        }

        return result;
    }

    private void generateRestockAlert(ForecastResult forecast) {
        RestockAlert alert = RestockAlert.of(
                forecast.productId(),
                forecast.productName(),
                forecast.currentStock(),
                forecast.recommendedQuantity(),
                forecast.daysUntilStockout(),
                forecast.avgDailyDemand(),
                forecast.confidenceScore()
        );
        restockAlertRepository.save(alert);

        RestockRecommendedEvent event = RestockRecommendedEvent.of(
                forecast.productId(),
                forecast.productName(),
                forecast.currentStock(),
                forecast.recommendedQuantity(),
                forecast.daysUntilStockout(),
                forecast.confidenceScore()
        );
        kafkaTemplate.send(KafkaTopics.ML_RESTOCK_RECOMMENDED, forecast.productId().toString(), event);

        log.info("Restock alert generated for {}: {} units recommended, {:.1f} days until stockout (confidence: {:.0f}%)",
                forecast.productName(), forecast.recommendedQuantity(),
                forecast.daysUntilStockout(), forecast.confidenceScore() * 100);
    }

    /**
     * Computes WMA over the last WMA_PERIOD days of the series.
     */
    private double computeWeightedMovingAverage(List<Integer> series) {
        int n = Math.min(WMA_PERIOD, series.size());
        List<Integer> recent = series.subList(series.size() - n, series.size());

        int weightSum = n * (n + 1) / 2;
        double weightedSum = 0;
        for (int i = 0; i < recent.size(); i++) {
            weightedSum += (double) recent.get(i) * (i + 1); // weight increases for more recent
        }
        return weightSum > 0 ? weightedSum / weightSum : 0;
    }

    private double computeStdDev(List<Integer> series, double mean) {
        double variance = series.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    public List<RestockAlert> getPendingAlerts() {
        return restockAlertRepository.findByAcknowledgedFalseOrderByDaysUntilStockoutAsc();
    }

    public Optional<RestockAlert> getLatestAlertForProduct(UUID productId) {
        return restockAlertRepository.findTopByProductIdOrderByGeneratedAtDesc(productId);
    }

    public record ForecastResult(
            UUID productId,
            String productName,
            int currentStock,
            double avgDailyDemand,
            double daysUntilStockout,
            int recommendedQuantity,
            double confidenceScore) {

        public static ForecastResult noData(UUID productId, String productName, int currentStock) {
            return new ForecastResult(productId, productName, currentStock, 0, Double.MAX_VALUE, 0, 0);
        }

        public boolean hasInsufficientData() {
            return avgDailyDemand == 0;
        }
    }
}
