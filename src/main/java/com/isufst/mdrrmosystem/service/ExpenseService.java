package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.Expense;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BudgetCategoryRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
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
    private final FindAuthenticatedUser findAuthenticatedUser;

    public ExpenseService(BudgetCategoryRepository budgetCategoryRepository, ExpenseRepository expenseRepository,
                          IncidentRepository incidentRepository,  FindAuthenticatedUser findAuthenticatedUser) {
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.expenseRepository = expenseRepository;
        this.incidentRepository = incidentRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Transactional
    public ExpenseResponse addExpense(long categoryId, ExpenseRequest expenseRequest) {
        BudgetCategory category = budgetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Budget Category Not Found"));

        Expense expense = new Expense();
        expense.setDescription(expenseRequest.description());
        expense.setAmount(expenseRequest.amount());
        expense.setExpenseDate(expenseRequest.expenseDate());
        expense.setCategory(category);

        if(expenseRequest.incidentId() != null){
            Incident incident = incidentRepository.findById(expenseRequest.incidentId())
                    .orElseThrow(() -> new RuntimeException("Incident Not Found"));

            expense.setIncident(incident);
        }

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
