package com.isufst.mdrrmosystem.request;

public record ApprovalRequestCreateRequest(
        String requestType,
        String title,
        String description,
        String referenceType,
        Long referenceId,
        String payloadJson
) {}
