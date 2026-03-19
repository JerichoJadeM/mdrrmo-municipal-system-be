package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetForecastResponse(
        int year,
        double totalForecast,
        double recommendedAdjustment,
        String assumptions,
        List<BudgetForecastCategoryResponse> categories
) {
}
