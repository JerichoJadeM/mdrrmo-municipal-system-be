package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.WeatherData;
import com.isufst.mdrrmosystem.repository.WeatherDataRepository;
import com.isufst.mdrrmosystem.request.WeatherRequest;
import com.isufst.mdrrmosystem.response.WeatherResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeatherService {

    private final WeatherDataRepository weatherDataRepository;

    public WeatherService(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;
    }

    @Transactional
    public WeatherResponse saveWeather(WeatherRequest weatherRequest) {

        WeatherData weather = new WeatherData();

        weather.setRecordedAt(weatherRequest.recordedAt());
        weather.setRainfall(weatherRequest.rainfall());
        weather.setTemperature(weatherRequest.temperature());
        weather.setWindSpeed(weatherRequest.windSpeed());
        weather.setCondition(weatherRequest.condition());

        WeatherData saved = weatherDataRepository.save(weather);

        return new WeatherResponse(
                saved.getId(),
                saved.getRecordedAt(),
                saved.getRainfall(),
                saved.getTemperature(),
                saved.getWindSpeed(),
                saved.getCondition()
        );
    }

    public WeatherResponse latestWeather() {
        WeatherData weather = weatherDataRepository.findTopByOrderByRecordedAtDesc();

        return  new  WeatherResponse(
                weather.getId(),
                weather.getRecordedAt(),
                weather.getRainfall(),
                weather.getTemperature(),
                weather.getWindSpeed(),
                weather.getCondition()

        );
    }
}
