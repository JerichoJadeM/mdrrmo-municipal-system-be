package com.isufst.mdrrmosystem.request;

public record NotificationCreateRequest(
        Long recipientUserId,
        String type,
        String title,
        String message,
        String referenceType,
        Long referenceId
) {}
