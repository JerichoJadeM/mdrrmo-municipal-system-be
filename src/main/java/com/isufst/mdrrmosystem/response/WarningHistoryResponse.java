package com.isufst.mdrrmosystem.response;

public record WarningHistoryResponse(
        String title,
        String message,
        String status,
        String recordedAt
) {
}
