package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record WeatherAlertResponse(
        String title,
        String severity,
        String affectedArea,
        String message,
        LocalDateTime issuedAt,
        String source
) {
}
