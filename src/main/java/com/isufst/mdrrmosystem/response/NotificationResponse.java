package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String referenceType,
        Long referenceId,
        Boolean isRead,
        LocalDateTime createdAt
) {}
