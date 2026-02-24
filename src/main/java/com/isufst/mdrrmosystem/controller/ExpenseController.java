package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.ExpenseRequest;
import com.isufst.mdrrmosystem.response.ExpenseResponse;
import com.isufst.mdrrmosystem.service.ExpenseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories/{categoryId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ExpenseResponse addExpense(@PathVariable long categoryId,
                                      @RequestBody ExpenseRequest expenseRequest) {
        return expenseService.addExpense(categoryId, expenseRequest);
    }
}
