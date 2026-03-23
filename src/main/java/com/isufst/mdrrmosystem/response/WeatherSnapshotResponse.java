package com.isufst.mdrrmosystem.response;

public record WeatherSnapshotResponse(
        double rainfall,
        double temperature,
        double windSpeed,
        String condition
) {
}
