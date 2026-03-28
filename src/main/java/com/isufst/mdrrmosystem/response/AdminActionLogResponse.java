package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record AdminActionLogResponse(
        Long id,
        Long actorUserId,
        String actorName,
        Long targetUserId,
        String targetUserName,
        String actionType,
        String description,
        LocalDateTime createdAt
) {}
