package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.time.LocalDateTime;

public record IncidentResponse(
        long id,
        String type,
        Long barangayId,
        String barangay,
        String severity,
        String status,
        LocalDateTime reportedAt,
        String description,
        Long assignedResponderId,
        String assignedResponderName
) { }
