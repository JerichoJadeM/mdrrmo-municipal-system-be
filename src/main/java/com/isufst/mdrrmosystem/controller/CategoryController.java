package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.CategoryRequest;
import com.isufst.mdrrmosystem.response.CategoryResponse;
import com.isufst.mdrrmosystem.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets/{budgetId}/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public CategoryResponse addCategory(@PathVariable long budgetId,
                                        @RequestBody CategoryRequest categoryRequest) {
        return categoryService.addCategory(budgetId, categoryRequest);
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@PathVariable long budgetId) {
        return categoryService.getCategoriesByBudget(budgetId);
    }
}
