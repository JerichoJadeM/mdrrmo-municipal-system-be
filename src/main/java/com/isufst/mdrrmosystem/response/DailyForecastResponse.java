package com.isufst.mdrrmosystem.response;

import java.time.LocalDate;

public record DailyForecastResponse(
        LocalDate forecastDate,
        String condition,
        Double minTemperature,
        Double maxTemperature,
        Double rainfallMm,
        Double rainfallProbability,
        Double windSpeed,
        String riskLevel,
        String advisory
) {
}
