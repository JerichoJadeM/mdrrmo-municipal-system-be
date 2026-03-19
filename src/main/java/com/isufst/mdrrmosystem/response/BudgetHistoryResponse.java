package com.isufst.mdrrmosystem.response;

public record BudgetHistoryResponse(
        int year,
        double allotment,
        double obligations,
        double remainingBalance
) {
}
