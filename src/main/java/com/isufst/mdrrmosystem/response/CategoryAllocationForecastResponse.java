package com.isufst.mdrrmosystem.response;

public record CategoryAllocationForecastResponse(
        String section,
        String category,
        double forecastAllocation,
        double historicalBaseline,
        double trendAdjustment,
        double ruleBasedAmount,
        double historicalAdjustment,
        double priceAdjustment,
        double contingencyAmount,
        String note
) { }