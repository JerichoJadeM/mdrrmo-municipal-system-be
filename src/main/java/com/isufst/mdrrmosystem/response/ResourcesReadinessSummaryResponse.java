package com.isufst.mdrrmosystem.response;

import java.util.List;

public record ResourcesReadinessSummaryResponse(
        String inventoryRiskLevel,
        long inventoryLowStockCount,
        long inventoryOutOfStockCount,
        String reliefRiskLevel,
        long reliefLowStockCount,
        long estimatedFamilyCoverage,
        String evacuationRiskLevel,
        long activeCentersCount,
        long nearFullCentersCount,
        long fullCentersCount,
        int overallOccupancyRate,
        String budgetRiskLevel,
        int budgetUtilizationRate,
        double forecastGap,
        String overallReadinessRiskLevel,
        int overallReadinessScore,
        List<String> warnings
) {
}
