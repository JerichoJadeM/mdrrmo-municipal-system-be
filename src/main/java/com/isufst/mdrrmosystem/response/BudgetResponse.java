package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.User;

import java.util.List;

public record BudgetResponse(
        long id,
        int year,
        double totalAmount,
        String description,
        List<CategoryResponse> categories,
        String createdBy
) {}