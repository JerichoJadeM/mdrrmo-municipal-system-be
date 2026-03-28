package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        long senderUserId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        Boolean pinned,
        LocalDateTime editedAt
) {}
