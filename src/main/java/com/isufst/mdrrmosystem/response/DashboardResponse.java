package com.isufst.mdrrmosystem.response;

import java.util.List;

public record DashboardResponse(
        Double totalBudget,
        Double totalSpent,
        Double remainingBudget,
        Long totalCategories,
        Long totalExpenses,
        List<CategoryBreakdownResponse> categoryBreakdown
) {
}
