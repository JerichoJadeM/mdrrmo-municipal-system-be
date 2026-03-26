package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.DashboardOverviewResponse;
import com.isufst.mdrrmosystem.response.DashboardResponse;
import com.isufst.mdrrmosystem.response.DashboardSummaryResponse;
import com.isufst.mdrrmosystem.service.DashboardOverviewService;
import com.isufst.mdrrmosystem.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardOverviewService dashboardOverviewService;

    public DashboardController(DashboardService dashboardService,
                               DashboardOverviewService dashboardOverviewService) {
        this.dashboardService = dashboardService;
        this.dashboardOverviewService = dashboardOverviewService;
    }

    @GetMapping("/{budgetId}")
    public DashboardResponse getDashboard(@PathVariable Long budgetId) {
        return dashboardService.getDashboard(budgetId);
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse getOverview() {
        return dashboardOverviewService.getOverview();
    }
}
