package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.request.BudgetRequest;
import com.isufst.mdrrmosystem.response.BudgetResponse;
import com.isufst.mdrrmosystem.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> getAllBudgets() {
        return budgetService.getAllBudget();
    }

    @PostMapping
    public Budget createBudget(@Valid @RequestBody BudgetRequest budget) {
        return budgetService.createBudget(budget);
    }

    @GetMapping("/{id}/spent")
    public double getTotalSpent(@PathVariable long id){
        return budgetService.getTotalSpent(id);
    }

    @GetMapping("/{id}/remaining")
    public double getRemaining(@PathVariable long id){
        return budgetService.getRemainingBudget(id);
    }
}
