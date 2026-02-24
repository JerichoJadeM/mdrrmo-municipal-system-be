package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.Expense;
import com.isufst.mdrrmosystem.repository.BudgetCategoryRepository;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.response.CategoryBreakdownResponse;
import com.isufst.mdrrmosystem.response.DashboardResponse;
import com.isufst.mdrrmosystem.response.DashboardSummaryResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final CalamityRepository calamityRepository;

    public DashboardService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository,
                            BudgetCategoryRepository categoryRepository,  CalamityRepository calamityRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.calamityRepository = calamityRepository;
    }

    public DashboardSummaryResponse getSummary() {
        double totalBudget = budgetRepository.findAll()
                .stream()
                .mapToDouble(Budget::getTotalAmount)
                .sum();

        double totalSpent = expenseRepository.findAll()
                .stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        double remaining = totalBudget - totalSpent;

        long categoryCount =  categoryRepository.count();
        long expenseCount = expenseRepository.count();

        List<CategoryBreakdownResponse> breakdown = expenseRepository.getCategoryBreakdown();

        long calamityCount = calamityRepository.countByDateBetween(
                LocalDate.now().withDayOfYear(1),
                LocalDate.now()
        );

        return new DashboardSummaryResponse(
                totalBudget,
                totalSpent,
                remaining,
                categoryCount,
                expenseCount,
                breakdown,
                calamityCount
        );
    }

    public DashboardResponse getDashboard(Long budgetId) {

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        Double totalSpent = expenseRepository.sumByBudgetId(budgetId);
        Double remaining = budget.getTotalAmount() - totalSpent;

        Long totalCategories = categoryRepository.countByBudgetId(budgetId);
        Long totalExpenses = expenseRepository.countByCategory_Budget_Id(budgetId);

        List<CategoryBreakdownResponse> breakdown = expenseRepository.getCategoryBreakdown();

        return new DashboardResponse(
                budget.getTotalAmount(),
                totalSpent,
                remaining,
                totalCategories,
                totalExpenses,
                breakdown
        );
    }

    public List<CategoryBreakdownResponse> getCategoryBreakdown() {
        List<BudgetCategory> categories = categoryRepository.findAll();

        return categories.stream()
                .map(category -> {
                    double total = category.getExpenses()
                            .stream()
                            .mapToDouble(Expense::getAmount)
                            .sum();

                    return  new  CategoryBreakdownResponse(
                            category.getName(),
                            total
                    );
                })
                .toList();
    }
}
