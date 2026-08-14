package com.isufst.mdrrmosystem.request;

import java.util.List;

public record DispatchIncidentRequest(
        Long responderId,
        String description,
        String overrideReason,
        List<ResourceUsageItemRequest> selectedResources,
        Double actualCost
) {
}
