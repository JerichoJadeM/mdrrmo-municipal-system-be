package com.isufst.mdrrmosystem.response;

public record RiskLevelResponse(
        String barangay,
        String riskLevel,
        String recommendation
) {
}
