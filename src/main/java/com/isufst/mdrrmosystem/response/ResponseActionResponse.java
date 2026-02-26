package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record ResponseActionResponse(
        Long id,
        String actionType,
        String description,
        LocalDateTime actionTime,
        String incidentType,
        String responderName
) {
}
