package com.isufst.mdrrmosystem.response;

public record BudgetForecastCategoryResponse(
        String section,
        String category,
        double amount,
        String note
) {
}
