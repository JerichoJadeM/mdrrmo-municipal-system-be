package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.response.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardOverviewService {

    private final DashboardService dashboardService;
    private final WeatherForecastService weatherForecastService;
    private final ResourcesReadinessService resourcesReadinessService;
    private final AlertsWarningsService alertsWarningsService;
    private final BudgetService budgetService;
    private final BudgetAnalyticsService budgetAnalyticsService;
    private final IncidentService incidentService;
    private final CalamityService calamityService;
    private final EvacuationCenterService evacuationCenterService;
    private final UserService userService;
    private final ReportsService reportsService;

    public DashboardOverviewService(DashboardService dashboardService,
                                    WeatherForecastService weatherForecastService,
                                    ResourcesReadinessService resourcesReadinessService,
                                    AlertsWarningsService alertsWarningsService,
                                    BudgetService budgetService,
                                    BudgetAnalyticsService budgetAnalyticsService,
                                    IncidentService incidentService,
                                    CalamityService calamityService,
                                    EvacuationCenterService evacuationCenterService,
                                    UserService userService,
                                    ReportsService reportsService) {
        this.dashboardService = dashboardService;
        this.weatherForecastService = weatherForecastService;
        this.resourcesReadinessService = resourcesReadinessService;
        this.alertsWarningsService = alertsWarningsService;
        this.budgetService = budgetService;
        this.budgetAnalyticsService = budgetAnalyticsService;
        this.incidentService = incidentService;
        this.calamityService = calamityService;
        this.evacuationCenterService = evacuationCenterService;
        this.userService = userService;
        this.reportsService = reportsService;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        DashboardSummaryResponse summary = dashboardService.getSummary();
        MunicipalWeatherForecastResponse weather = weatherForecastService.getMunicipalForecast();
        ResourcesReadinessSummaryResponse readiness = resourcesReadinessService.getReadinessSummary();
        AlertsWarningsOverviewResponse alerts = alertsWarningsService.getOverview();
        BudgetCurrentSummaryResponse budgetCurrent = budgetService.getCurrentSummary();
        List<BudgetHistoryResponse> budgetHistory = budgetAnalyticsService.getBudgetHistory(5);
        List<IncidentResponse> incidents = incidentService.getAllIncidents();
        List<CalamityResponse> calamities = calamityService.getAllCalamityRecords();
        List<EvacuationCenterResourceResponse> evacuationCenters =
                evacuationCenterService.getResourcesView(null, null, null);
        List<ResponderResponse> responders = userService.searchAvailableResponders("");

        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        List<AuditTrailResponse> recentActivity =
                reportsService.getAuditTrail(null, null, null, null, from, to, null);

        return new DashboardOverviewResponse(
                LocalDateTime.now(),
                alerts.summary() != null ? alerts.summary().overallReadinessLabel() : null,
                summary,
                weather,
                readiness,
                alerts,
                budgetCurrent,
                budgetHistory,
                incidents,
                calamities,
                evacuationCenters,
                responders,
                recentActivity
        );
    }
}
