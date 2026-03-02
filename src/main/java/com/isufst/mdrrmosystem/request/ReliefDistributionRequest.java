package com.isufst.mdrrmosystem.request;

public record ReliefDistributionRequest(
        String itemType,
        int quantity,
        Long distributedById,
        Long evacuationActivationId // nullable
) {
}
