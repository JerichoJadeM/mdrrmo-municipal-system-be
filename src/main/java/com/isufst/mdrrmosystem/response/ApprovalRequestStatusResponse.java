package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record ApprovalRequestStatusResponse(
        Long id,
        String status,
        Long requestedById,
        String requestedByName,
        Long reviewedById,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewRemarks
) {}
