package com.isufst.mdrrmosystem.response;

public record BudgetCurrentSummaryResponse(
        int year,
        double totalAllotment,
        double totalObligations,
        double remainingBalance,
        double utilizationRate,
        double allocatedToCategories,
        double unallocatedBudget
) {
}
