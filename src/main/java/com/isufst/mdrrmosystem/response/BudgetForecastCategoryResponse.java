package com.isufst.mdrrmosystem.response;

public record BudgetForecastCategoryResponse(
        String section,
        String category,
        double historicalBaseline,
        double trendAdjustment,
        double ruleBasedAmount,
        double historicalAdjustment,
        double priceAdjustment,
        double contingencyAmount,
        double finalAmount,
        String note
) { }