package com.isufst.mdrrmosystem.request;

import java.time.LocalDate;

public record InventoryCreateProcurementRequest(
        String name,
        String category,
        String unit,
        String location,
        Integer reorderLevel,
        Boolean criticalItem,
        Double estimatedUnitCost,
        Long categoryId,
        Integer quantityAdded,
        Double totalCost,
        LocalDate expenseDate,
        Long incidentId,
        Long calamityId,
        String description
) {
}
