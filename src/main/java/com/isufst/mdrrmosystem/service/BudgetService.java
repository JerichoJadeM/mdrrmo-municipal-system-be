package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.request.BudgetRequest;
import com.isufst.mdrrmosystem.response.*;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final InventoryRepository inventoryRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final CategoryService categoryService;

    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         InventoryRepository inventoryRepository,
                         FindAuthenticatedUser findAuthenticatedUser,
                         CategoryService categoryService) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.inventoryRepository = inventoryRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudget() {
        return budgetRepository.findAllByOrderByYearAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetCurrentSummaryResponse getCurrentSummary() {
        int currentYear = LocalDate.now().getYear();

        Budget budget = budgetRepository.findFirstByYear(currentYear)
                .orElseThrow(() -> new RuntimeException("Current year budget not found."));

        return buildCurrentSummary(budget);
    }

    @Transactional(readOnly = true)
    public BudgetDetailResponse getBudgetDetail(long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double totalAllotment = budget.getTotalAmount();
        double totalAllocated = budget.getCategories() == null
                ? 0
                : budget.getCategories().stream().mapToDouble(BudgetCategory::getAllocatedAmount).sum();
        double totalObligations = expenseRepository.sumByBudgetId(budget.getId());
        double totalRemaining = totalAllotment - totalObligations;

        double allocationRate = totalAllotment > 0 ? (totalAllocated / totalAllotment) * 100 : 0;
        double utilizationRate = totalAllotment > 0 ? (totalObligations / totalAllotment) * 100 : 0;

        return new BudgetDetailResponse(
                budget.getId(),
                budget.getYear(),
                budget.getTotalAmount(),
                budget.getDescription(),
                totalAllocated,
                totalObligations,
                totalRemaining,
                allocationRate,
                utilizationRate,
                categoryService.getCategoriesByBudget(budget.getId()),
                budget.getCreatedBy() != null
                        ? budget.getCreatedBy().getFirstName() + " " + budget.getCreatedBy().getLastName()
                        : null
        );
    }

    @Transactional
    public Budget createBudget(BudgetRequest request) {
        int currentYear = LocalDate.now().getYear();

        if (request.getYear() < currentYear) {
            throw new RuntimeException("Cannot create a budget for a past year.");
        }

        User user = findAuthenticatedUser.getAuthenticatedUser();

        Budget budget = budgetRepository.findFirstByYear(request.getYear())
                .orElseGet(Budget::new);

        boolean isNew = budget.getId() == 0;

        if (isNew) {
            budget.setYear(request.getYear());
            budget.setCreateAt(LocalDate.now());
            budget.setCreatedBy(user);
            budget.setDescription(request.getDescription());
            budget.setTotalAmount(request.getTotalAmount());
        } else {
            budget.setTotalAmount(budget.getTotalAmount() + request.getTotalAmount());

            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                budget.setDescription(request.getDescription().trim());
            }
        }

        return budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public double getTotalSpent(long budgetId) {
        return expenseRepository.sumByBudgetId(budgetId);
    }

    @Transactional(readOnly = true)
    public double getRemainingBudget(long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double totalSpent = getTotalSpent(budgetId);
        return budget.getTotalAmount() - totalSpent;
    }

    @Transactional(readOnly = true)
    public List<BudgetHistoryResponse> getBudgetHistory(int years) {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - Math.max(years - 1, 0);

        return budgetRepository.findAllByOrderByYearAsc().stream()
                .filter(budget -> budget.getYear() >= startYear && budget.getYear() <= currentYear)
                .map(budget -> {
                    double obligations = expenseRepository.sumByBudgetId(budget.getId());
                    double remaining = budget.getTotalAmount() - obligations;
                    double utilization = budget.getTotalAmount() > 0
                            ? (obligations / budget.getTotalAmount()) * 100
                            : 0;

                    return new BudgetHistoryResponse(
                            budget.getYear(),
                            budget.getTotalAmount(),
                            obligations,
                            remaining,
                            utilization
                    );
                })
                .toList();
    }

    // This one is hybrid and will over forecast when other categories are missing
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

        double fiveYearAverageAllotment = historicalBudgets.stream()
                .mapToDouble(Budget::getTotalAmount)
                .average()
                .orElse(currentBudget.getTotalAmount());

        Map<String, List<BudgetCategory>> groupedHistory = new HashMap<>();

        for (Budget budget : historicalBudgets) {
            if (budget.getCategories() == null) continue;
            for (BudgetCategory category : budget.getCategories()) {
                String key = key(category.getSection(), category.getName());
                groupedHistory.computeIfAbsent(key, ignored -> new ArrayList<>()).add(category);
            }
        }

        List<ForecastTemplateRow> templateRows = getWorkbookForecastTemplate();

        List<BudgetForecastCategoryResponse> rows = new ArrayList<>();
        double totalForecast = 0;

        for (ForecastTemplateRow templateRow : templateRows) {
            String key = key(templateRow.section(), templateRow.category());
            List<BudgetCategory> categoryHistory = groupedHistory.getOrDefault(key, List.of());

            double historicalBaseline;
            if (!categoryHistory.isEmpty()) {
                historicalBaseline = categoryHistory.stream()
                        .mapToDouble(BudgetCategory::getAllocatedAmount)
                        .average()
                        .orElse(0);
            } else {
                double fallbackWeight = templateRow.defaultWeight();
                historicalBaseline = fiveYearAverageAllotment * fallbackWeight;
            }

            double oldestAllocation = categoryHistory.isEmpty()
                    ? historicalBaseline
                    : categoryHistory.get(0).getAllocatedAmount();

            double latestAllocation = categoryHistory.isEmpty()
                    ? historicalBaseline
                    : categoryHistory.get(categoryHistory.size() - 1).getAllocatedAmount();

            double trendAdjustment = oldestAllocation > 0
                    ? ((latestAllocation - oldestAllocation) / oldestAllocation) * historicalBaseline
                    : 0;

            double historicalObligationAverage = categoryHistory.stream()
                    .mapToDouble(category -> expenseRepository.sumByCategoryId(category.getId()))
                    .average()
                    .orElse(0);

            double historicalAdjustment = historicalObligationAverage * 0.20;
            double ruleBasedAmount = estimateRuleBasedAmount(templateRow.section(), templateRow.category(), historicalBaseline);
            double priceAdjustment = estimatePriceAdjustment(templateRow.section(), templateRow.category());
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
                    templateRow.section(),
                    templateRow.category(),
                    historicalBaseline,
                    trendAdjustment,
                    ruleBasedAmount,
                    historicalAdjustment,
                    priceAdjustment,
                    contingency,
                    finalAmount,
                    "5-year baseline + trend + obligations + workbook-aligned fallback + rule-based pressure + price adjustment + contingency."
            ));
        }

        List<BudgetForecastDriverResponse> drivers = List.of(
                new BudgetForecastDriverResponse(
                        "Historical Budget Window",
                        startYear + " - " + currentYear,
                        "Previous 5 years are used as the required forecast basis."
                ),
                new BudgetForecastDriverResponse(
                        "Workbook Category Template",
                        "Included",
                        "Forecast remains aligned to workbook sections and categories."
                ),
                new BudgetForecastDriverResponse(
                        "Historical Obligations",
                        "Included",
                        "Actual expenses from incidents and calamities influence next-year forecast."
                ),
                new BudgetForecastDriverResponse(
                        "Rule-Based Pressure",
                        "Included",
                        "Response, relief, evacuation, medical, and preparedness logic adjusts forecast demand."
                ),
                new BudgetForecastDriverResponse(
                        "Price Signal",
                        "Included",
                        "Inventory estimated unit costs affect forecast adjustments."
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
                "Hybrid forecast aligned to workbook categories, 5-year budget history, actual obligations, disaster-response pressure, and price signals.",
                drivers,
                rows.stream()
                        .sorted(Comparator.comparing(BudgetForecastCategoryResponse::section)
                                .thenComparing(BudgetForecastCategoryResponse::category))
                        .toList()
        );
    }

    private BudgetCurrentSummaryResponse buildCurrentSummary(Budget budget) {
        double totalAllotment = budget.getTotalAmount();

        double totalAllocated = budget.getCategories() == null
                ? 0
                : budget.getCategories().stream()
                .mapToDouble(BudgetCategory::getAllocatedAmount)
                .sum();

        double totalObligations = expenseRepository.sumByBudgetId(budget.getId());
        double totalRemaining = totalAllotment - totalObligations;

        double allocationRate = totalAllotment > 0
                ? (totalAllocated / totalAllotment) * 100
                : 0;

        double utilizationRate = totalAllotment > 0
                ? (totalObligations / totalAllotment) * 100
                : 0;

        return new BudgetCurrentSummaryResponse(
                budget.getId(),
                budget.getYear(),
                totalAllotment,
                totalAllocated,
                totalObligations,
                totalRemaining,
                allocationRate,
                utilizationRate,
                budget.getDescription()
        );
    }

    private BudgetResponse mapToResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getYear(),
                budget.getTotalAmount(),
                budget.getDescription(),
                categoryService.getCategoriesByBudget(budget.getId()),
                budget.getCreatedBy() != null
                        ? budget.getCreatedBy().getFirstName() + " " + budget.getCreatedBy().getLastName()
                        : null
        );
    }

    private String key(String section, String category) {
        return safe(section) + "|" + safe(category);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private double estimateRuleBasedAmount(String section, String category, double historicalBaseline) {
        String safeSection = safe(section);
        String safeCategory = safe(category);

        if ("DISASTER RESPONSE".equals(safeSection)) {
            if (safeCategory.contains("FOOD") || safeCategory.contains("WATER")) return historicalBaseline * 0.15;
            if (safeCategory.contains("MEDICAL")) return historicalBaseline * 0.12;
            if (safeCategory.contains("EVAC")) return historicalBaseline * 0.18;
            if (safeCategory.contains("DRUG")) return historicalBaseline * 0.10;
            return historicalBaseline * 0.10;
        }

        if ("DISASTER PREPAREDNESS".equals(safeSection)) {
            if (safeCategory.contains("EQUIPMENT")) return historicalBaseline * 0.10;
            if (safeCategory.contains("TRAINING")) return historicalBaseline * 0.06;
            if (safeCategory.contains("TRAVEL")) return historicalBaseline * 0.04;
            return historicalBaseline * 0.05;
        }

        if ("DISASTER PREVENTION AND MITIGATION".equals(safeSection)) {
            if (safeCategory.contains("CAPITAL")) return historicalBaseline * 0.08;
            return historicalBaseline * 0.05;
        }

        if ("DISASTER REHABILITATION AND RECOVERY".equals(safeSection)) {
            return historicalBaseline * 0.06;
        }

        return historicalBaseline * 0.05;
    }

    private double estimatePriceAdjustment(String section, String category) {
        List<Inventory> inventory = inventoryRepository.findAll();

        double averageCost = inventory.stream()
                .filter(i -> i.getEstimatedUnitCost() != null && i.getEstimatedUnitCost() > 0)
                .mapToDouble(Inventory::getEstimatedUnitCost)
                .average()
                .orElse(0);

        if (averageCost <= 0) return 0;

        String safeSection = safe(section);
        String safeCategory = safe(category);

        if ("DISASTER RESPONSE".equals(safeSection)) {
            if (safeCategory.contains("FOOD") || safeCategory.contains("WATER")) return averageCost * 25;
            if (safeCategory.contains("MEDICAL")) return averageCost * 18;
            if (safeCategory.contains("EVAC")) return averageCost * 15;
            return averageCost * 10;
        }

        if ("DISASTER PREPAREDNESS".equals(safeSection) && safeCategory.contains("EQUIPMENT")) {
            return averageCost * 12;
        }

        return averageCost * 5;
    }

    @Transactional(readOnly = true)
    public BudgetCurrentSummaryResponse getSummaryByYear(int year) {
        Budget budget = budgetRepository.findFirstByYear(year)
                .orElseThrow(() -> new RuntimeException("Budget not found for year: " + year));

        return buildCurrentSummary(budget);
    }

    private List<ForecastTemplateRow> getWorkbookForecastTemplate() {
        return List.of(
                new ForecastTemplateRow("DISASTER PREPAREDNESS", "Training Expenses", 0.08),
                new ForecastTemplateRow("DISASTER PREPAREDNESS", "Traveling Expenses", 0.04),
                new ForecastTemplateRow("DISASTER PREPAREDNESS", "Rescue Equipment", 0.07),
                new ForecastTemplateRow("DISASTER PREVENTION AND MITIGATION", "Other Supplies and Materials", 0.08),
                new ForecastTemplateRow("DISASTER PREVENTION AND MITIGATION", "Capital Outlay", 0.10),
                new ForecastTemplateRow("DISASTER RESPONSE", "Food and Water", 0.14),
                new ForecastTemplateRow("DISASTER RESPONSE", "Medical Supplies", 0.08),
                new ForecastTemplateRow("DISASTER RESPONSE", "Drugs and Medicines Expenses", 0.07),
                new ForecastTemplateRow("DISASTER RESPONSE", "Evacuation Support", 0.08),
                new ForecastTemplateRow("DISASTER RESPONSE", "Maintenance and Other Operating Expenses", 0.10),
                new ForecastTemplateRow("DISASTER REHABILITATION AND RECOVERY", "Subsidy to Other Funds", 0.08),
                new ForecastTemplateRow("DISASTER REHABILITATION AND RECOVERY", "Other Supplies and Materials", 0.08)
        );
    }

    private record ForecastTemplateRow(String section, String category, double defaultWeight) {}
}