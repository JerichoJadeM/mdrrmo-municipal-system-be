package com.isufst.mdrrmosystem.response;

public record ResourceRecommendationResponse(
        String resourceType,
        String itemName,
        String category,
        int suggestedQuantity,
        String unit,
        String reason
) {
}