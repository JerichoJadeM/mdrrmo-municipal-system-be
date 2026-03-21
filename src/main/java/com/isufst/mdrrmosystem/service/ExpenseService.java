package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ExpenseRequest;
import com.isufst.mdrrmosystem.response.ExpenseResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    public ExpenseService(BudgetCategoryRepository budgetCategoryRepository,
                          ExpenseRepository expenseRepository,
                          IncidentRepository incidentRepository,
                          CalamityRepository calamityRepository,
                          FindAuthenticatedUser findAuthenticatedUser) {
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.expenseRepository = expenseRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Transactional
    public ExpenseResponse addExpense(long categoryId, ExpenseRequest expenseRequest) {
        BudgetCategory category = budgetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Budget Category Not Found"));

        if (expenseRequest.incidentId() != null && expenseRequest.calamityId() != null) {
            throw new RuntimeException("Expense can only be linked to one operation: incident or calamity.");
        }

        Incident incident = null;
        if (expenseRequest.incidentId() != null) {
            incident = incidentRepository.findById(expenseRequest.incidentId())
                    .orElseThrow(() -> new RuntimeException("Incident Not Found"));
        }

        Calamity calamity = null;
        if (expenseRequest.calamityId() != null) {
            calamity = calamityRepository.findById(expenseRequest.calamityId())
                    .orElseThrow(() -> new RuntimeException("Calamity Not Found"));
        }

        Expense expense = new Expense();
        expense.setDescription(expenseRequest.description());
        expense.setAmount(expenseRequest.amount());
        expense.setExpenseDate(expenseRequest.expenseDate());
        expense.setCategory(category);
        expense.setIncident(incident);
        expense.setCalamity(calamity);
        expense.setCreatedBy(findAuthenticatedUser.getAuthenticatedUser());

        expenseRepository.save(expense);

        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getExpenseDate()
        );
    }
}