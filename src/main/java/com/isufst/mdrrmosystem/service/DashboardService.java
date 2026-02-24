package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.repository.BudgetCategoryRepository;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.response.DashboardResponse;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetCategoryRepository categoryRepository;

    public DashboardService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository,BudgetCategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    public DashboardResponse getDashboard(Long budgetId) {

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        Double totalSpent = expenseRepository.sumByBudgetId(budgetId);
        Double remaining = budget.getTotalAmount() - totalSpent;

        Long totalCategories = categoryRepository.countByBudgetId(budgetId);
        Long totalExpenses = expenseRepository.countByCategory_Budget_Id(budgetId);

        return new DashboardResponse(
                budget.getTotalAmount(),
                totalSpent,
                remaining,
                totalCategories,
                totalExpenses
        );
    }
}
