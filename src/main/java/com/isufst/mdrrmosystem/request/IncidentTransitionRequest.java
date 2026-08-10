package com.isufst.mdrrmosystem.request;

import java.util.List;

/**
 * Body accepted by incident status-transition endpoints that previously took no payload
 * (arrive, resolve). {@code actualCost} is a client-computed hint only; the backend recomputes
 * the authoritative cost from {@code selectedResources} against its own inventory pricing.
 */
public record IncidentTransitionRequest(
        String description,
        String overrideReason,
        List<ResourceUsageItemRequest> selectedResources,
        Double actualCost
) {
}
