package com.isufst.mdrrmosystem.response;

public record ReportsSummaryResponse(
        long totalIncidents,
        long activeIncidents,
        long resolvedIncidents,
        long totalCalamities,
        long activeCalamities,
        long resolvedCalamities,
        long totalInventoryItems,
        long lowStockItems,
        long openEvacuationCenters,
        long totalAuditEvents,
        double currentYearBudget,
        double currentYearSpent,
        double currentYearRemaining
) {
}
