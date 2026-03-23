package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;
import java.util.List;

public record MunicipalWeatherForecastResponse(
        String municipality,
        String province,
        String source,
        LocalDateTime generatedAt,
        WeatherSnapshotResponse current,
        WeatherSummaryResponse summary,
        List<DailyForecastResponse> dailyForecasts,
        List<BarangayRiskForecastResponse> barangayRisks,
        List<WeatherAlertResponse> alerts
) {
}
