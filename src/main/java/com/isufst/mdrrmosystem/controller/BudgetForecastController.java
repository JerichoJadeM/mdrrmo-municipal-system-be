package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.BudgetForecastResponse;
import com.isufst.mdrrmosystem.service.BudgetForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets/forecast")
public class BudgetForecastController {

    private final BudgetForecastService budgetForecastService;

    public BudgetForecastController(BudgetForecastService budgetForecastService) {
        this.budgetForecastService = budgetForecastService;
    }

    @GetMapping("/next-year")
    public BudgetForecastResponse getNextYearForecast() {
        return budgetForecastService.getNextYearForecast();
    }
}