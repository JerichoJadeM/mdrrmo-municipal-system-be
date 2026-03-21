package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetAnalyticsResponse(
        int year,
        double totalAllotment,
        double totalObligations,
        double totalRemaining,
        double utilizationRate,
        List<BudgetSectionAnalyticsResponse> sectionTotals,
        List<BudgetCategoryAnalyticsResponse> categoryTotals,
        List<OperationCostAnalyticsResponse> incidentCosts,
        List<OperationCostAnalyticsResponse> calamityCosts
) { }