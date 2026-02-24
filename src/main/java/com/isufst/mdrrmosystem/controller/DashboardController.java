package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.DashboardResponse;
import com.isufst.mdrrmosystem.response.DashboardSummaryResponse;
import com.isufst.mdrrmosystem.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{budgetId}")
    public DashboardResponse getDashboard(@PathVariable Long budgetId) {
        return dashboardService.getDashboard(budgetId);
    }

    @GetMapping("summary")
    public DashboardSummaryResponse getSummary(){
        return dashboardService.getSummary();
    }
}
