package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetDetailResponse(
        long id,
        int year,
        double totalAmount,
        String description,
        double totalAllocated,
        double totalObligations,
        double totalRemaining,
        double allocationRate,
        double utilizationRate,
        List<CategoryResponse> categories,
        String createdBy
) { }