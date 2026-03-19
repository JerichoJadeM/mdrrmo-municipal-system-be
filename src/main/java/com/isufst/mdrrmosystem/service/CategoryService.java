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

        double alreadyAllocated = budget.getCategories() == null
                ? 0
                : budget.getCategories().stream()
                .mapToDouble(BudgetCategory::getAllocatedAmount)
                .sum();

        double remainingUnallocated = budget.getTotalAmount() - alreadyAllocated;

        if (categoryRequest.allocatedAmount() <= 0) {
            throw new RuntimeException("Allocated amount must be greater than 0");
        }

        if (categoryRequest.allocatedAmount() > remainingUnallocated) {
            throw new RuntimeException("Allocated amount exceeds remaining unallocated budget");
        }

        BudgetCategory category = new BudgetCategory();
        category.setName(categoryRequest.name());
        category.setSection(normalizeSection(categoryRequest.section()));
        category.setAllocatedAmount(categoryRequest.allocatedAmount());
        category.setBudget(budget);

        BudgetCategory saved = budgetCategoryRepository.save(category);

        return new CategoryResponse(
                saved.getId(),
                saved.getName(),
                saved.getSection(),
                saved.getAllocatedAmount()
        );
    }

    private String normalizeSection(String value) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Budget section is required");
        }

        return value.trim().toUpperCase();
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
                        category.getName(),
                        category.getSection() != null ? category.getSection() : "UNASSIGNED",
                        category.getAllocatedAmount()
                ))
                .toList();
    }
}
