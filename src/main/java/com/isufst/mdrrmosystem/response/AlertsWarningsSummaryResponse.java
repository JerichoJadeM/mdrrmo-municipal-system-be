package com.isufst.mdrrmosystem.response;

public record AlertsWarningsSummaryResponse(
        String overallReadinessLabel,
        int activeWarningsCount,
        int criticalGapsCount,
        String responseCapacityLabel
) {
}
