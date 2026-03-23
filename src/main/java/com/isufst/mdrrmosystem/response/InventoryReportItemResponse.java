package com.isufst.mdrrmosystem.response;

public record InventoryReportItemResponse(
        Long id,
        String itemName,
        String category,
        Integer availableQuantity,
        Integer reorderLevel,
        String stockStatus
) {
}
