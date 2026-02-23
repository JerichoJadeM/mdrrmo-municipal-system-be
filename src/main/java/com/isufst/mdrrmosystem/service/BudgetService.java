package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.request.BudgetRequest;
import com.isufst.mdrrmosystem.response.BudgetResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    public BudgetService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository, FindAuthenticatedUser findAuthenticatedUser) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudget(){

        return budgetRepository.findAll().stream()
                .map(budget -> new BudgetResponse(
                        budget.getId(),
                        budget.getYear(),
                        budget.getTotalAmount(),
                        budget.getDescription(),
                        budget.getCategories(),
                        budget.getCreatedBy().getFirstName() + " " + budget.getCreatedBy().getLastName()
                ))
                .toList();
    }

    public Budget createBudget(BudgetRequest request) {
        User user = findAuthenticatedUser.getAuthenticatedUser();

        Budget budget = new Budget();

        budget.setYear(request.getYear());
        budget.setTotalAmount(request.getTotalAmount());
        budget.setDescription(request.getDescription());
        budget.setCreateAt(LocalDate.now());
        budget.setCreatedBy(user);

        return budgetRepository.save(budget);
    }

    public double getTotalSpent(long budgetId) {
        return  expenseRepository.sumByBudgetId(budgetId);
    }

    public double getRemainingBudget(long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double totalSpent = getTotalSpent(budgetId);

        return budget.getTotalAmount() - totalSpent;
    }
}
