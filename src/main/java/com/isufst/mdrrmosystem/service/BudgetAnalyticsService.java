package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.response.*;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetAnalyticsService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;

    public BudgetAnalyticsService(BudgetRepository budgetRepository,
                                  ExpenseRepository expenseRepository,
                                  IncidentRepository incidentRepository,
                                  CalamityRepository calamityRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
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

    @Transactional(readOnly = true)
    public BudgetAnalyticsResponse getBudgetAnalytics(int year) {
        Budget budget = budgetRepository.findFirstByYear(year)
                .orElseThrow(() -> new RuntimeException("Budget not found for year: " + year));

        double totalObligations = expenseRepository.sumByBudgetId(budget.getId());
        double totalRemaining = budget.getTotalAmount() - totalObligations;
        double utilizationRate = budget.getTotalAmount() > 0
                ? (totalObligations / budget.getTotalAmount()) * 100
                : 0;

        List<BudgetCategoryAnalyticsResponse> categoryTotals = budget.getCategories().stream()
                .map(category -> {
                    double obligated = expenseRepository.sumByCategoryId(category.getId());
                    double remaining = category.getAllocatedAmount() - obligated;
                    double utilization = category.getAllocatedAmount() > 0
                            ? (obligated / category.getAllocatedAmount()) * 100
                            : 0;

                    return new BudgetCategoryAnalyticsResponse(
                            category.getId(),
                            category.getSection(),
                            category.getName(),
                            category.getAllocatedAmount(),
                            obligated,
                            remaining,
                            utilization
                    );
                })
                .sorted(Comparator.comparing(BudgetCategoryAnalyticsResponse::section)
                        .thenComparing(BudgetCategoryAnalyticsResponse::categoryName))
                .toList();

        List<BudgetSectionAnalyticsResponse> sectionTotals = categoryTotals.stream()
                .collect(Collectors.groupingBy(BudgetCategoryAnalyticsResponse::section))
                .entrySet()
                .stream()
                .map(entry -> {
                    double allocated = entry.getValue().stream().mapToDouble(BudgetCategoryAnalyticsResponse::allocatedAmount).sum();
                    double obligated = entry.getValue().stream().mapToDouble(BudgetCategoryAnalyticsResponse::obligatedAmount).sum();
                    double remaining = allocated - obligated;
                    double utilization = allocated > 0 ? (obligated / allocated) * 100 : 0;

                    return new BudgetSectionAnalyticsResponse(
                            entry.getKey(),
                            allocated,
                            obligated,
                            remaining,
                            utilization
                    );
                })
                .sorted(Comparator.comparing(BudgetSectionAnalyticsResponse::section))
                .toList();

        List<OperationCostAnalyticsResponse> incidentCosts = incidentRepository.findAll().stream()
                .map(incident -> new OperationCostAnalyticsResponse(
                        "INCIDENT",
                        incident.getId(),
                        incident.getType(),
                        expenseRepository.sumByIncidentId(incident.getId())
                ))
                .filter(item -> item.totalCost() > 0)
                .toList();

        List<OperationCostAnalyticsResponse> calamityCosts = calamityRepository.findAll().stream()
                .map(calamity -> new OperationCostAnalyticsResponse(
                        "CALAMITY",
                        calamity.getId(),
                        calamity.getType(),
                        expenseRepository.sumByCalamityId(calamity.getId())
                ))
                .filter(item -> item.totalCost() > 0)
                .toList();

        return new BudgetAnalyticsResponse(
                budget.getYear(),
                budget.getTotalAmount(),
                totalObligations,
                totalRemaining,
                utilizationRate,
                sectionTotals,
                categoryTotals,
                incidentCosts,
                calamityCosts
        );
    }
}