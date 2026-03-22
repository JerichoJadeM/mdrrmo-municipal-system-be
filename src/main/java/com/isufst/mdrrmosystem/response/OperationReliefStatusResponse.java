package com.isufst.mdrrmosystem.response;

import java.util.List;

public record OperationReliefStatusResponse(
        String eventType,
        Long eventId,

        boolean reliefRecommended,
        int projectedBeneficiaries,
        int projectedReliefPacks,

        int distributedReliefPacks,
        int remainingReliefPacks,
        boolean needsAdditionalRelief,

        String status,

        Long packTemplateId,
        String packTemplateName,
        String packType,
        String basis,

        List<OperationReliefDistributedItemResponse> distributedItems,
        List<OperationReliefLackingItemResponse> lackingItems,
        List<ReliefDistributionResponse> distributions
) {
}