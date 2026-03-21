package com.isufst.mdrrmosystem.response;

public record ReliefPackReadinessItemResponse(
        Long inventoryId,
        String inventoryName,
        int quantityRequiredPerPack,
        int availableQuantity,
        int produciblePacksFromThisItem,
        boolean limitingItem,
        Double estimatedUnitCost
) {
}
