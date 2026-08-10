package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.PreviousBudget;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.PreviousBudgetRepository;
import com.isufst.mdrrmosystem.request.PreviousBudgetRequest;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private FindAuthenticatedUser findAuthenticatedUser;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PreviousBudgetRepository previousBudgetRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void createPreviousBudget_shouldComputeCorrectRemainingAndUtilization() {
        PreviousBudgetRequest request = new PreviousBudgetRequest(
                2025,
                1500000.0,
                1350000.0,
                150000.0,
                90.0,
                "Previous budget sample"
        );

        when(previousBudgetRepository.existsByYear(2025)).thenReturn(false);
        when(previousBudgetRepository.save(any(PreviousBudget.class))).thenAnswer(invocation -> {
            PreviousBudget saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PreviousBudget created = budgetService.createPreviousBudget(request);

        assertEquals(150000.0, created.getRemaining(), 0.01);
        assertEquals(90.0, created.getUtilizationRate(), 0.01);
        assertEquals(1500000.0, created.getAllotment(), 0.01);
        assertEquals(1350000.0, created.getObligations(), 0.01);
    }
}
