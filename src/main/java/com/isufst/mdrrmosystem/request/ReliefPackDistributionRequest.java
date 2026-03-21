package com.isufst.mdrrmosystem.request;

public record ReliefPackDistributionRequest(
        Integer packCount,
        Long evacuationActivationId
) {
}
