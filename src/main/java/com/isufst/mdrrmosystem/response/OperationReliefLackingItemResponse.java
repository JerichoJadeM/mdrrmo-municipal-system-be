package com.isufst.mdrrmosystem.response;

public record OperationReliefLackingItemResponse(
        Long inventoryId,
        String inventoryName,
        String unit,
        int quantityRequiredPerPack,
        int availableQuantity,
        int requiredQuantityForRemainingPacks,
        int lackingQuantity,
        boolean limitingItem
) {
}