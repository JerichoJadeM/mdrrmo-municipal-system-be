package com.isufst.mdrrmosystem.request;

import java.time.LocalDate;

public record InventoryProcurementRequest(
        Long categoryId,
        Integer quantityAdded,
        Double totalCost,
        LocalDate expenseDate,
        String description,
        Long incidentId,
        Long calamityId
) {
}
