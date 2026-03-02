package com.isufst.mdrrmosystem.response;

public record InventoryResponse(
        Long id,
        String name,
        String category,
        int totalQuantity,
        int availableQuantity,
        String unit,
        String location
) {
}
