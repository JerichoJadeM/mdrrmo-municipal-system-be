package com.isufst.mdrrmosystem.external.weather;

import com.isufst.mdrrmosystem.external.weather.dto.ExternalWeatherPayload;

public interface WeatherProvider {

    ExternalWeatherPayload fetchBatadWeather();
}
