package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record ReliefDistributionReportItemResponse(
        Long id,
        String referenceType,
        Long referenceId,
        String itemName,
        Integer quantity,
        String distributedBy,
        LocalDateTime distributedAt
) {}
