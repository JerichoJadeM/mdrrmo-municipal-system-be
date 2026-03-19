package com.isufst.mdrrmosystem.response;

public record ResourcesSummaryResponse(
        long inventoryCount,
        long lowStockInventoryCount,
        double budgetRemaining,
        double budgetUsed,
        long reliefReadyCount,
        long reliefLowStockCount,
        long activeCentersCount,
        int centerOccupancyRate
) {
}
