package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.response.BudgetForecastCategoryResponse;
import com.isufst.mdrrmosystem.response.BudgetForecastDriverResponse;
import com.isufst.mdrrmosystem.response.BudgetForecastResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
// Official next-year forecast used by Resources, Reports, and exports.
// This is the single source of truth for operational financial reporting.
@Service
public class BudgetForecastService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final InventoryRepository inventoryRepository;

    public BudgetForecastService(BudgetRepository budgetRepository,
                                 ExpenseRepository expenseRepository,
                                 InventoryRepository inventoryRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public BudgetForecastResponse getNextYearForecast() {
        int currentYear = LocalDate.now().getYear();
        int nextYear = currentYear + 1;
        int startYear = currentYear - 4;

        List<Budget> historicalBudgets = budgetRepository.findAllByOrderByYearAsc().stream()
                .filter(b -> b.getYear() >= startYear && b.getYear() <= currentYear)
                .toList();

        if (historicalBudgets.isEmpty()) {
            throw new RuntimeException("No budget history found for the previous 5 years.");
        }

        Budget currentBudget = historicalBudgets.stream()
                .filter(b -> b.getYear() == currentYear)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Current year budget not found."));

        Map<String, List<BudgetCategory>> groupedHistory = new HashMap<>();

        for (Budget budget : historicalBudgets) {
            if (budget.getCategories() == null) continue;
            for (BudgetCategory category : budget.getCategories()) {
                String key = key(category.getSection(), category.getName());
                groupedHistory.computeIfAbsent(key, ignored -> new ArrayList<>()).add(category);
            }
        }

        List<BudgetForecastCategoryResponse> rows = new ArrayList<>();
        double totalForecast = 0;

        for (BudgetCategory currentCategory : currentBudget.getCategories()) {
            String key = key(currentCategory.getSection(), currentCategory.getName());
            List<BudgetCategory> categoryHistory = groupedHistory.getOrDefault(key, List.of());

            double historicalBaseline = categoryHistory.stream()
                    .mapToDouble(BudgetCategory::getAllocatedAmount)
                    .average()
                    .orElse(currentCategory.getAllocatedAmount());

            double oldestAllocation = categoryHistory.isEmpty()
                    ? currentCategory.getAllocatedAmount()
                    : categoryHistory.get(0).getAllocatedAmount();

            double latestAllocation = currentCategory.getAllocatedAmount();
            double trendAdjustment = oldestAllocation > 0
                    ? ((latestAllocation - oldestAllocation) / oldestAllocation) * historicalBaseline
                    : 0;

            double historicalObligationAverage = categoryHistory.stream()
                    .mapToDouble(category -> expenseRepository.sumByCategoryId(category.getId()))
                    .average()
                    .orElse(0);

            double historicalAdjustment = historicalObligationAverage * 0.20;
            double ruleBasedAmount = estimateRuleBasedAmount(currentCategory, historicalBaseline);
            double priceAdjustment = estimatePriceAdjustment(currentCategory);
            double contingency = (historicalBaseline + trendAdjustment + historicalAdjustment + ruleBasedAmount + priceAdjustment) * 0.10;

            double finalAmount = historicalBaseline
                    + trendAdjustment
                    + historicalAdjustment
                    + ruleBasedAmount
                    + priceAdjustment
                    + contingency;

            if (finalAmount < 0) {
                finalAmount = 0;
            }

            totalForecast += finalAmount;

            rows.add(new BudgetForecastCategoryResponse(
                    currentCategory.getSection(),
                    currentCategory.getName(),
                    historicalBaseline,
                    trendAdjustment,
                    ruleBasedAmount,
                    historicalAdjustment,
                    priceAdjustment,
                    contingency,
                    finalAmount,
                    "5-year baseline + trend + obligations + rule-based pressure + price adjustment + contingency."
            ));
        }

        List<BudgetForecastDriverResponse> drivers = List.of(
                new BudgetForecastDriverResponse(
                        "Historical Budget Window",
                        startYear + " - " + currentYear,
                        "Previous 5 years are used as the required baseline."
                ),
                new BudgetForecastDriverResponse(
                        "Historical Obligations",
                        "Included",
                        "Actual expenses from incidents and calamities are part of the forecast."
                ),
                new BudgetForecastDriverResponse(
                        "Operations", // ruled based pressure
                        "Included",
                        "Response, relief, evacuation, medical, and preparedness logic adjusts forecast demand."
                ),
                new BudgetForecastDriverResponse(
                        "Recent Price Signal",
                        "Included",
                        "Inventory estimated unit costs and procurement price signals influence forecast."
                ),
                new BudgetForecastDriverResponse(
                        "Contingency",
                        "10%",
                        "Applied as uncertainty allowance."
                )
        );

        return new BudgetForecastResponse(
                nextYear,
                totalForecast,
                "Forecast aligned to budget history, actual obligations, response demand, and latest price signals.",
                drivers,
                rows.stream()
                        .sorted(Comparator.comparing(BudgetForecastCategoryResponse::section)
                                .thenComparing(BudgetForecastCategoryResponse::category))
                        .toList()
        );
    }

    private String key(String section, String category) {
        return (section == null ? "" : section.trim().toUpperCase()) + "|" +
                (category == null ? "" : category.trim().toUpperCase());
    }

    private double estimateRuleBasedAmount(BudgetCategory category, double historicalBaseline) {
        String section = safe(category.getSection());
        String name = safe(category.getName());

        if ("DISASTER_RESPONSE".equals(section)) {
            if (name.contains("FOOD") || name.contains("WATER")) return historicalBaseline * 0.15;
            if (name.contains("MEDICAL")) return historicalBaseline * 0.12;
            if (name.contains("EVAC")) return historicalBaseline * 0.18;
            if (name.contains("RELIEF")) return historicalBaseline * 0.14;
            return historicalBaseline * 0.10;
        }

        if ("DISASTER_PREPAREDNESS".equals(section)) {
            if (name.contains("EQUIPMENT")) return historicalBaseline * 0.10;
            if (name.contains("TRAINING")) return historicalBaseline * 0.06;
            return historicalBaseline * 0.05;
        }

        if ("DISASTER_PREVENTION_AND_MITIGATION".equals(section)) {
            if (name.contains("CAPITAL")) return historicalBaseline * 0.08;
            return historicalBaseline * 0.05;
        }

        if ("DISASTER_REHABILITATION_AND_RECOVERY".equals(section)) {
            return historicalBaseline * 0.06;
        }

        return historicalBaseline * 0.05;
    }

    private double estimatePriceAdjustment(BudgetCategory category) {
        List<Inventory> inventory = inventoryRepository.findAll();

        double averageCost = inventory.stream()
                .filter(i -> i.getEstimatedUnitCost() != null && i.getEstimatedUnitCost() > 0)
                .mapToDouble(Inventory::getEstimatedUnitCost)
                .average()
                .orElse(0);

        if (averageCost <= 0) return 0;

        String section = safe(category.getSection());
        String name = safe(category.getName());

        if ("DISASTER_RESPONSE".equals(section)) {
            if (name.contains("FOOD") || name.contains("WATER")) return averageCost * 25;
            if (name.contains("MEDICAL")) return averageCost * 18;
            if (name.contains("EVAC")) return averageCost * 15;
            return averageCost * 10;
        }

        if ("DISASTER_PREPAREDNESS".equals(section) && name.contains("EQUIPMENT")) {
            return averageCost * 12;
        }

        return averageCost * 5;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}