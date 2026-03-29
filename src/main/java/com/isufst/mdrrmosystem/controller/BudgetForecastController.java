package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.BudgetForecastBreakdownResponse;
import com.isufst.mdrrmosystem.response.BudgetForecastResponse;
import com.isufst.mdrrmosystem.service.BudgetForecastBreakdownService;
import com.isufst.mdrrmosystem.service.BudgetForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets/forecast")
public class BudgetForecastController {

    private final BudgetForecastService budgetForecastService;
    private final BudgetForecastBreakdownService budgetForecastBreakdownService;

    public BudgetForecastController(BudgetForecastService budgetForecastService,
                                    BudgetForecastBreakdownService budgetForecastBreakdownService) {
        this.budgetForecastService = budgetForecastService;
        this.budgetForecastBreakdownService = budgetForecastBreakdownService;
    }

    @GetMapping("/next-year")
    public BudgetForecastResponse getNextYearForecast() {
        return budgetForecastService.getNextYearForecast();
    }

    @GetMapping("/next-year/breakdown")
    public BudgetForecastBreakdownResponse getNextYearForecastBreakdown() {
        return budgetForecastBreakdownService.getNextYearForecastBreakdown();
    }
}