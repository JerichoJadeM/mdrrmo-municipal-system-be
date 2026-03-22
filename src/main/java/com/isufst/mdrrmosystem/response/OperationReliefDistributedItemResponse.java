package com.isufst.mdrrmosystem.response;

public record OperationReliefDistributedItemResponse(
        Long inventoryId,
        String inventoryName,
        int distributedQuantity,
        String unit
) {
}