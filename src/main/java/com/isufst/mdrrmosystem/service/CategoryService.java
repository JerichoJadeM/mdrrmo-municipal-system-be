package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.repository.BudgetCategoryRepository;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.request.CategoryRequest;
import com.isufst.mdrrmosystem.response.CategoryResponse;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;

    public CategoryService(BudgetRepository budgetRepository, BudgetCategoryRepository budgetCategoryRepository) {
        this.budgetRepository = budgetRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
    }

    public CategoryResponse addCategory(long budgetId, CategoryRequest categoryRequest) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        BudgetCategory category = new BudgetCategory();
        category.setName(categoryRequest.name());
        category.setAllocatedAmount(categoryRequest.allocatedAmount());
        category.setBudget(budget);

        budgetCategoryRepository.save(category);

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getAllocatedAmount()
        );
    }
}
