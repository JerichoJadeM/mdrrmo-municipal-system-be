package com.isufst.mdrrmosystem.response;

public record BudgetWarningResponse(
        String level,
        String message
) {
}