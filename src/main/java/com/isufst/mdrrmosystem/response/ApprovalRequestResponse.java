package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record ApprovalRequestResponse(
        Long id,
        String requestType,
        String status,
        Long requestedByUserId,
        String requestedByName,
        Long reviewedByUserId,
        String reviewedByName,
        String title,
        String description,
        String referenceType,
        Long referenceId,
        String payloadJson,
        String reviewRemarks,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {}
