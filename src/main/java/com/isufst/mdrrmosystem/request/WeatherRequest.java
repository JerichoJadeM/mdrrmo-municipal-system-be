package com.isufst.mdrrmosystem.request;

import java.time.LocalDateTime;

public record WeatherRequest(
        LocalDateTime recordedAt,
        double rainfall,
        double temperature,
        double windSpeed,
        String condition
) {
}
