package com.isufst.mdrrmosystem.request;

public record ReliefDistributionRequest(
        Long inventoryId,
        int quantity,
        Long distributedById,
        Long evacuationActivationId // nullable
) {
}
