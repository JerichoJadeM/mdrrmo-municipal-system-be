package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetForecastBreakdownResponse(
        int year,
        double overallForecast,
        double incidentForecastTotal,
        double calamityForecastTotal,
        double incidentSharePercent,
        double calamitySharePercent,
        List<OperationTypeForecastResponse> incidentTypeForecasts,
        List<OperationTypeForecastResponse> calamityTypeForecasts,
        List<CategoryAllocationForecastResponse> categoryAllocationForecasts
) { }
