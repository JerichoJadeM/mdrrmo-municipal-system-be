package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.WeatherRequest;
import com.isufst.mdrrmosystem.response.WeatherResponse;
import com.isufst.mdrrmosystem.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    // Insert weather data (manual or API)
    @PostMapping
    public WeatherResponse addWeather(@RequestBody WeatherRequest weatherRequest) {
        return weatherService.saveWeather(weatherRequest);
    }

    // Get latest weather (dashboard use)
    @GetMapping
    public WeatherResponse latest(){
        return weatherService.latestWeather();
    }


}
