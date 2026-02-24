package com.isufst.mdrrmosystem.response;

import java.time.LocalDate;

public record ExpenseResponse(
        long id,
        String description,
        double amount,
        LocalDate expenseDate
) {
}
