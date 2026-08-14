package com.isufst.mdrrmosystem.request;

/**
 * A single resource line item selected by the frontend during a status-transition review
 * (dispatch/arrive/resolve/monitor/end). {@code inventoryId} is preferred for matching;
 * {@code itemName} is a fallback when the id isn't available. Any client-supplied unit cost
 * is intentionally NOT accepted here — the backend always recomputes cost server-side from
 * the inventory's own {@code estimatedUnitCost} (OWASP: never trust client-supplied financial data).
 */
public record ResourceUsageItemRequest(
        Long inventoryId,
        String itemName,
        Integer quantity
) {
}
