package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record AuditTrailResponse(
        Long id,
        String module,
        String recordType,
        Long recordId,
        String actionType,
        String fromStatus,
        String toStatus,
        String description,
        String metadataJson,
        String performedBy,
        LocalDateTime performedAt
) {
}
