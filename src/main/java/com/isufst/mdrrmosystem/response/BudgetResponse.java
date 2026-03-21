package com.isufst.mdrrmosystem.response;

import java.util.List;

public record BudgetResponse(
        long id,
        int year,
        double totalAmount,
        String description,
        List<CategoryResponse> categories,
        String createdBy
) { }