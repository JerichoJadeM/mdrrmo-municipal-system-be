package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record WeatherResponse(
        Long id,
        LocalDateTime recordedAt,
        double rainfall,
        double temperature,
        double windSpeed,
        String condition
) {
}
