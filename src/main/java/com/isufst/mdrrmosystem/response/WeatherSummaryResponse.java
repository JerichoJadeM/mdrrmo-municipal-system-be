package com.isufst.mdrrmosystem.response;

public record WeatherSummaryResponse(
        String overallRiskLevel,
        String rainfallOutlook,
        String recommendation,
        int totalBarangays,
        int highRiskBarangays,
        int mediumRiskBarangays,
        int lowRiskBarangays
) {
}
