package com.isufst.mdrrmosystem.response;

public record BudgetSectionAnalyticsResponse(
        String section,
        double allocatedAmount,
        double obligatedAmount,
        double remainingAmount,
        double utilizationRate
) { }