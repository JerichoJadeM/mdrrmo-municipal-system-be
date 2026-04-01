package com.isufst.mdrrmosystem.response;

public record TopConsumedResourceResponse(
        String itemName,
        Long usedQuantity
) {}
