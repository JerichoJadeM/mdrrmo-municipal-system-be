package com.isufst.mdrrmosystem.response;

import java.util.List;

public record FinancialReportResponse(
        int year,
        BudgetCurrentSummaryResponse currentSummary,
        List<BudgetHistoryResponse> history,
        BudgetForecastResponse nextYearForecast,
        BudgetAnalyticsResponse analytics
) {
}
