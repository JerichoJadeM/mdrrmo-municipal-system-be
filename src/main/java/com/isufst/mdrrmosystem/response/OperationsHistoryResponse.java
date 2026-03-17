package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record OperationsHistoryResponse(
        Long id,
        String operationType,
        Long operationId,
        String actionType,
        String fromStatus,
        String toStatus,
        String description,
        String metadataJson,
        String performedBy,
        LocalDateTime performedAt
) {
}
