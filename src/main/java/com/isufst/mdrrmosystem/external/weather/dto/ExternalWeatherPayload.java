package com.isufst.mdrrmosystem.external.weather.dto;

import com.isufst.mdrrmosystem.response.DailyForecastResponse;
import com.isufst.mdrrmosystem.response.WeatherAlertResponse;
import com.isufst.mdrrmosystem.response.WeatherSnapshotResponse;

import java.util.List;

public record ExternalWeatherPayload(
        String source,
        WeatherSnapshotResponse current,
        List<DailyForecastResponse> dailyForecasts,
        List<WeatherAlertResponse> alerts
) {
}
