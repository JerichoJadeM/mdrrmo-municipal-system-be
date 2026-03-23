package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.MunicipalWeatherForecastResponse;
import com.isufst.mdrrmosystem.service.WeatherForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather-forecast")
public class WeatherForecastController {

    private final WeatherForecastService weatherForecastService;

    public WeatherForecastController(WeatherForecastService weatherForecastService) {
        this.weatherForecastService = weatherForecastService;
    }

    @GetMapping("/municipal")
    public MunicipalWeatherForecastResponse getMunicipalForecast() {
        return weatherForecastService.getMunicipalForecast();
    }
}