package com.isufst.mdrrmosystem.response;

public record ReliefPackTemplateItemResponse(
        Long id,
        Long inventoryId,
        String inventoryName,
        String unit,
        int quantityRequired,
        int availableQuantity,
        Double estimatedUnitCost
) {
}
