package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.BudgetAnalyticsResponse;
import com.isufst.mdrrmosystem.response.BudgetHistoryResponse;
import com.isufst.mdrrmosystem.service.BudgetAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetAnalyticsController {

    private final BudgetAnalyticsService budgetAnalyticsService;

    public BudgetAnalyticsController(BudgetAnalyticsService budgetAnalyticsService) {
        this.budgetAnalyticsService = budgetAnalyticsService;
    }

    @GetMapping("/history")
    public List<BudgetHistoryResponse> getHistory() {
        return budgetAnalyticsService.getBudgetHistory(5);
    }

    @GetMapping("/{year}/analytics")
    public BudgetAnalyticsResponse getAnalytics(@PathVariable int year) {
        return budgetAnalyticsService.getBudgetAnalytics(year);
    }
}