package com.isufst.mdrrmosystem.response;

public record BudgetCategoryAnalyticsResponse(
        Long categoryId,
        String section,
        String categoryName,
        double allocatedAmount,
        double obligatedAmount,
        double remainingAmount,
        double utilizationRate
) { }