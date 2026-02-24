package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.Expense;
import com.isufst.mdrrmosystem.repository.BudgetCategoryRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.request.ExpenseRequest;
import com.isufst.mdrrmosystem.response.ExpenseResponse;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseRepository expenseRepository;

    public ExpenseService(BudgetCategoryRepository budgetCategoryRepository, ExpenseRepository expenseRepository) {
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.expenseRepository = expenseRepository;
    }

    public ExpenseResponse addExpense(long categoryId, ExpenseRequest expenseRequest) {
        BudgetCategory category = budgetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Budget Category Not Found"));

        Expense expense = new Expense();
        expense.setDescription(expenseRequest.description());
        expense.setAmount(expenseRequest.amount());
        expense.setExpenseDate(expenseRequest.expenseDate());
        expense.setCategory(category);

        expenseRepository.save(expense);

        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getExpenseDate()
        );

    }
}
