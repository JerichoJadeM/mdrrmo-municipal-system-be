package com.isufst.mdrrmosystem.request;

import java.time.LocalDate;

public record ExpenseRequest(
        String description,
        double amount,
        LocalDate expenseDate,
        Long incidentId,
        Long calamityId
) {
}
