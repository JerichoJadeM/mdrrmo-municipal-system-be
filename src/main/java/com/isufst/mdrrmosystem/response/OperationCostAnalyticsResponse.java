package com.isufst.mdrrmosystem.response;

public record OperationCostAnalyticsResponse(
        String operationType,
        Long operationId,
        String operationLabel,
        double totalCost
) { }