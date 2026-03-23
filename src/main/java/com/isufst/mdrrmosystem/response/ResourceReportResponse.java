package com.isufst.mdrrmosystem.response;

import java.util.List;

public record ResourceReportResponse(
        long inventoryCount,
        long lowStockCount,
        long evacuationCenterCount,
        long openEvacuationCenters,
        long reliefDistributionCount,
        List<InventoryReportItemResponse> lowStockItems,
        List<InventoryTransactionReportResponse> inventoryTransactions,
        List<ReliefDistributionReportItemResponse> reliefDistributions
) {
}
