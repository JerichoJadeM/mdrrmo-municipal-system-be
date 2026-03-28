package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
        Long id,
        String displayName,
        String lastMessage,
        LocalDateTime updatedAt,
        int unreadCount
) {}
