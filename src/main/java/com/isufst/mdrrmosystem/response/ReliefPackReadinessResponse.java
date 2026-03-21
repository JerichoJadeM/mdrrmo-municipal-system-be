package com.isufst.mdrrmosystem.response;

import java.util.List;

public record ReliefPackReadinessResponse(
        Long templateId,
        String templateName,
        String packType,
        String intendedUse,
        int maxProduciblePacks,
        String limitingItemName,
        double estimatePackCost,
        boolean hasCompleteCostData,
        List<ReliefPackReadinessItemResponse> items
) {
}
