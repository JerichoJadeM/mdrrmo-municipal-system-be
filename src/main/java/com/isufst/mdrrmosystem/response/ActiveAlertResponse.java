package com.isufst.mdrrmosystem.response;

public record ActiveAlertResponse(
        String type,
        String severity,
        String status,
        String title,
        String message,
        String affectedArea,
        String issuedAt,
        String source,
        String recommendation
) {
}
