package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.repository.BudgetCategoryRepository;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.request.CategoryRequest;
import com.isufst.mdrrmosystem.response.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;

    public CategoryService(BudgetRepository budgetRepository,
                           BudgetCategoryRepository budgetCategoryRepository) {
        this.budgetRepository = budgetRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
    }

    @Transactional
    public CategoryResponse addCategory(long budgetId, CategoryRequest categoryRequest) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double currentAllocated = budget.getCategories() == null
                ? 0
                : budget.getCategories().stream()
                .mapToDouble(BudgetCategory::getAllocatedAmount)
                .sum();

        double remainingBudget = budget.getTotalAmount() - currentAllocated;

        if (categoryRequest.allocatedAmount() > remainingBudget) {
            throw new RuntimeException("Allocated amount exceeds remaining budget.");
        }

        BudgetCategory category = new BudgetCategory();
        category.setSection(categoryRequest.section().trim().toUpperCase());
        category.setName(categoryRequest.name());
        category.setAllocatedAmount(categoryRequest.allocatedAmount());
        category.setBudget(budget);

        budgetCategoryRepository.save(category);

        return new CategoryResponse(
                category.getId(),
                category.getSection(),
                category.getName(),
                category.getAllocatedAmount()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByBudget(long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (budget.getCategories() == null) {
            return List.of();
        }

        return budget.getCategories().stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getSection(),
                        category.getName(),
                        category.getAllocatedAmount()
                ))
                .toList();
    }
}