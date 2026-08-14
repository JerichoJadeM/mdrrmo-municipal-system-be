package com.isufst.mdrrmosystem.request;

import java.util.List;

public record CalamityTransitionRequest(
        String description,
        String overrideReason,
        List<ResourceUsageItemRequest> selectedResources,
        Double actualCost
) {
}
