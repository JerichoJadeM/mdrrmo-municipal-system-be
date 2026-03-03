package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record ReliefDistributionResponse(
        Long id,
        Long inventoryId,
        String inventoryName,
        int quantity,
        LocalDateTime distributedAt,
        String incidentType,
        String centerName,
        String distributedBy
) {
}
