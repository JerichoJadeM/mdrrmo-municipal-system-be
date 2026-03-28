package com.isufst.mdrrmosystem.request;

public record CreateConversationRequest(
        Long recipientUserId,
        String message
) {}