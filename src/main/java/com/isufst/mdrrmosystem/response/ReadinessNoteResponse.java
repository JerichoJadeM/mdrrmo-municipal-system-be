package com.isufst.mdrrmosystem.response;

public record ReadinessNoteResponse(
        String title,
        String message,
        String category
) {
}
