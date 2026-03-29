package com.isufst.mdrrmosystem.response;

public record ActionSubmissionResponse(
        boolean executed,
        boolean approvalRequired,
        String message,
        Long approvalRequestId
) {}
