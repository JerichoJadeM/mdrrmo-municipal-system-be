package com.isufst.mdrrmosystem.response;

public record DashboardResponse(
        Double totalBudget,
        Double totalSpent,
        Double remainingBudget,
        Long totalCategories,
        Long totalExpenses
) {
}
