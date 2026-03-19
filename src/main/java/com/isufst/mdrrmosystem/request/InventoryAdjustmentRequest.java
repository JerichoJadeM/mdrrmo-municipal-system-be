package com.isufst.mdrrmosystem.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryAdjustmentRequest(
        @NotBlank(message = "Action type is required")
        String actionType, // RESTOCK, DEPLOY, RETURN, CONSUMED, DAMAGED, ADJUSTMENT

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        Long incidentId,
        Long performedById,
        String note
) {
}
