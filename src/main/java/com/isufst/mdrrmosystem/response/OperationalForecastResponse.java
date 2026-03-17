package com.isufst.mdrrmosystem.response;

import java.util.List;

public record OperationalForecastResponse(
        String eventType,
        Long eventId,
        String title,
        String severity,
        String status,

        boolean evacuationRecommended,
        boolean reliefRecommended,

        double forecastedBudget,
        double actualCostToDate,
        double variance,

        List<ResourceRecommendationResponse> recommendedResources,
        List<StockCheckResponse> stockChecks,
        ReliefReadinessResponse reliefReadiness,
        List<EvacuationCheckResponse> evacuationChecks,
        List<CostDriverResponse> costDrivers,
        List<BudgetWarningResponse> warnings
) {
}