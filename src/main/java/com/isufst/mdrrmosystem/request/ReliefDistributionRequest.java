package com.isufst.mdrrmosystem.request;

public record ReliefDistributionRequest(
        Long inventoryId,
        Integer quantity,
        Long evacuationActivationId, // nullable
        Long calamityId
) {
}
