package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardOverviewResponse(
        LocalDateTime lastUpdated,
        String overallReadinessLabel,
        DashboardSummaryResponse summary,
        MunicipalWeatherForecastResponse weather,
        ResourcesReadinessSummaryResponse readiness,
        AlertsWarningsOverviewResponse alerts,
        BudgetCurrentSummaryResponse budgetCurrent,
        List<BudgetHistoryResponse> budgetHistory,
        List<IncidentResponse> incidents,
        List<CalamityResponse> calamities,
        List<EvacuationCenterResourceResponse> evacuationCenters,
        List<ResponderResponse> responders,
        List<AuditTrailResponse> recentActivity
) {
}
