package com.isufst.mdrrmosystem.response;

public record OperationTypeForecastResponse(
        String operationGroup,
        String type,
        long historicalCount,
        double historicalCost,
        double historicalAverageCost,
        double forecastAmount,
        double sharePercent,
        String note
) { }
