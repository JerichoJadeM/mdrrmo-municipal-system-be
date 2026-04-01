package com.isufst.mdrrmosystem.response;

public record BudgetCurrentSummaryResponse(
        Long budgetId,
        int year,
        double totalAllotment,
        double totalAllocated,
        double totalObligations,
        double totalRemaining,
        double allocationRate,
        Double utilizationRate,
        String description
) { }