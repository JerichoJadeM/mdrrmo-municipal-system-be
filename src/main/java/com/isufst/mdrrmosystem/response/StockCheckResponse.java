package com.isufst.mdrrmosystem.response;

public record StockCheckResponse(
        Long inventoryId,
        String itemName,
        String category,
        int requiredQuantity,
        int availableQuantity,
        String unit,
        String status
) {
}