package com.isufst.mdrrmosystem.response;

import java.util.List;

public record NextYearBudgetForecastResponse(
        int year,
        double totalForecast,
        double recommendedAdjustment,
        String assumptions,
        List<BudgetForecastCategoryResponse> categories
) {
}
