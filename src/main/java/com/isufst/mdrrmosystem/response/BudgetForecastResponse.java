package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetForecastResponse(
        int year,
        double totalForecast,
        String assumptions,
        List<BudgetForecastDriverResponse> drivers,
        List<BudgetForecastCategoryResponse> categories
) {
}
