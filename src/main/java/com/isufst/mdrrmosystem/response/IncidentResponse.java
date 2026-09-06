package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentResponse(
        long id,
        String type,
        Long barangayId,
        String barangay,
        String severity,
        String status,
        LocalDateTime reportedAt,
        String description,
        List<Long> assignedResponderIds,
        List<String> assignedResponderNames
) { }
