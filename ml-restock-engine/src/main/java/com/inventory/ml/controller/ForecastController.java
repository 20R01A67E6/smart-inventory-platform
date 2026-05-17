package com.inventory.ml.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.ml.model.RestockAlert;
import com.inventory.ml.service.DemandForecastingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    private final DemandForecastingService forecastingService;

    public ForecastController(DemandForecastingService forecastingService) {
        this.forecastingService = forecastingService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<RestockAlert>>> getPendingAlerts() {
        List<RestockAlert> alerts = forecastingService.getPendingAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/alerts/{productId}/latest")
    public ResponseEntity<ApiResponse<RestockAlert>> getLatestAlert(@PathVariable UUID productId) {
        return forecastingService.getLatestAlertForProduct(productId)
                .map(alert -> ResponseEntity.ok(ApiResponse.success(alert)))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponse.error("No restock alert found for product: " + productId)));
    }

    @PostMapping("/run/{productId}")
    public ResponseEntity<ApiResponse<DemandForecastingService.ForecastResult>> runForecast(
            @PathVariable UUID productId,
            @RequestParam String productName,
            @RequestParam int currentStock) {
        DemandForecastingService.ForecastResult result =
                forecastingService.forecastProduct(productId, productName, currentStock);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
