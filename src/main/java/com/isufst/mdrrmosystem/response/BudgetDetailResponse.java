package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetDetailResponse(
        long id,
        int year,
        double totalAllotment,
        double totalObligations,
        double remainingBalance,
        double utilizationRate,
        double allocatedToCategories,
        double unallocatedBudget,
        String description,
        List<CategoryResponse> categories,
        String createdBy
) {
}
