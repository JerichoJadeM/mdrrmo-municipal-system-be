package com.isufst.mdrrmosystem.response;

public record BarangayRiskForecastResponse(
        Long barangayId,
        String barangayName,
        boolean floodProne,
        boolean landslideProne,
        boolean coastal,
        String riskLevel,
        String reason,
        String recommendation
) {
}
