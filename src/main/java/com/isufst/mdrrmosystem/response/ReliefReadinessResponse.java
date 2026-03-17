package com.isufst.mdrrmosystem.response;

import java.util.List;

public record ReliefReadinessResponse(
        boolean recommended,
        int projectedBeneficiaries,
        int projectedReliefPacks,
        List<StockCheckResponse> reliefStockChecks
) {
}