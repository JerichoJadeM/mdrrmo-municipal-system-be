package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record IncidentReportItemResponse(
        Long id,
        String type,
        String status,
        String barangay,
        String location,
        LocalDateTime reportedAt
) {
}
