package com.isufst.mdrrmosystem.external.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.isufst.mdrrmosystem.external.weather.dto.ExternalWeatherPayload;
import com.isufst.mdrrmosystem.response.DailyForecastResponse;
import com.isufst.mdrrmosystem.response.WeatherAlertResponse;
import com.isufst.mdrrmosystem.response.WeatherSnapshotResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenMeteoWeatherProvider implements WeatherProvider {

    private final RestClient restClient;

    @Value("${weather.batad.latitude:11.4189}")
    private double latitude;

    @Value("${weather.batad.longitude:123.1230}")
    private double longitude;

    public OpenMeteoWeatherProvider() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public ExternalWeatherPayload fetchBatadWeather() {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,precipitation,wind_speed_10m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max"
                + "&timezone=auto"
                + "&forecast_days=7";

        OpenMeteoResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(OpenMeteoResponse.class);

        if (response == null || response.current == null || response.daily == null) {
            throw new IllegalStateException("Open-Meteo returned no usable data.");
        }

        WeatherSnapshotResponse current = new WeatherSnapshotResponse(
                safeDouble(response.current.precipitation),
                safeDouble(response.current.temperature2m),
                safeDouble(response.current.windSpeed10m),
                mapWeatherCode(response.current.weatherCode)
        );

        List<DailyForecastResponse> forecasts = new ArrayList<>();
        int days = response.daily.time == null ? 0 : response.daily.time.size();

        for (int i = 0; i < days; i++) {
            LocalDate date = response.daily.time.get(i);
            Double rain = getAt(response.daily.precipitationSum, i);
            Double rainProb = getAt(response.daily.precipitationProbabilityMax, i);
            Double wind = getAt(response.daily.windSpeed10mMax, i);
            Integer code = getAt(response.daily.weatherCode, i);
            String condition = mapWeatherCode(code);

            forecasts.add(new DailyForecastResponse(
                    date,
                    condition,
                    getAt(response.daily.temperature2mMin, i),
                    getAt(response.daily.temperature2mMax, i),
                    rain,
                    rainProb,
                    wind,
                    deriveRisk(rain, wind, condition),
                    deriveAdvisory(rain, wind, condition)
            ));
        }

        List<WeatherAlertResponse> alerts = buildAlerts(current, forecasts);

        return new ExternalWeatherPayload(
                "OPEN_METEO",
                current,
                forecasts,
                alerts
        );
    }

    private List<WeatherAlertResponse> buildAlerts(WeatherSnapshotResponse current,
                                                   List<DailyForecastResponse> forecasts) {
        List<WeatherAlertResponse> alerts = new ArrayList<>();

        double peakRain = forecasts.stream()
                .map(DailyForecastResponse::rainfallMm)
                .filter(v -> v != null)
                .max(Double::compareTo)
                .orElse(current.rainfall());

        if (peakRain >= 100) {
            alerts.add(new WeatherAlertResponse(
                    "Heavy Rainfall Advisory",
                    "HIGH",
                    "Whole Municipality",
                    "Heavy rainfall is possible in the forecast period. Monitor flood-prone areas and prepare response assets.",
                    LocalDateTime.now(),
                    "OPEN_METEO"
            ));
        }

        boolean stormy = forecasts.stream()
                .map(DailyForecastResponse::condition)
                .anyMatch(c -> c != null && c.toLowerCase().contains("thunder"));

        if (stormy) {
            alerts.add(new WeatherAlertResponse(
                    "Thunderstorm Advisory",
                    "MODERATE",
                    "Whole Municipality",
                    "Thunderstorms may occur during the forecast period. Monitor lightning risk and outdoor responder safety.",
                    LocalDateTime.now(),
                    "OPEN_METEO"
            ));
        }

        if (alerts.isEmpty()) {
            alerts.add(new WeatherAlertResponse(
                    "General Weather Monitoring",
                    "LOW",
                    "Whole Municipality",
                    "No major forecast-based weather alerts at this time.",
                    LocalDateTime.now(),
                    "OPEN_METEO"
            ));
        }

        return alerts;
    }

    private String deriveRisk(Double rainfall, Double windSpeed, String condition) {
        double rain = rainfall == null ? 0 : rainfall;
        double wind = windSpeed == null ? 0 : windSpeed;
        String c = condition == null ? "" : condition.toLowerCase();

        int score = 0;
        if (rain >= 100) score += 3;
        else if (rain >= 50) score += 2;
        else if (rain >= 20) score += 1;

        if (wind >= 35) score += 2;
        else if (wind >= 20) score += 1;

        if (c.contains("thunder")) score += 1;

        if (score >= 4) return "HIGH";
        if (score >= 2) return "MEDIUM";
        return "LOW";
    }

    private String deriveAdvisory(Double rainfall, Double windSpeed, String condition) {
        String risk = deriveRisk(rainfall, windSpeed, condition);
        if ("HIGH".equals(risk)) return "Prepare response assets and closely monitor vulnerable areas.";
        if ("MEDIUM".equals(risk)) return "Increase monitoring and review response readiness.";
        return "Continue normal weather monitoring.";
    }

    private String mapWeatherCode(Integer code) {
        if (code == null) return "Unknown";
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2 -> "Mostly sunny";
            case 3 -> "Cloudy";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Partly cloudy";
        };
    }

    private <T> T getAt(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    public static class OpenMeteoResponse {
        public Current current;
        public Daily daily;
    }

    public static class Current {
        @JsonProperty("temperature_2m")
        public Double temperature2m;

        public Double precipitation;

        @JsonProperty("wind_speed_10m")
        public Double windSpeed10m;

        @JsonProperty("weather_code")
        public Integer weatherCode;
    }

    public static class Daily {
        public List<LocalDate> time;

        @JsonProperty("weather_code")
        public List<Integer> weatherCode;

        @JsonProperty("temperature_2m_max")
        public List<Double> temperature2mMax;

        @JsonProperty("temperature_2m_min")
        public List<Double> temperature2mMin;

        @JsonProperty("precipitation_sum")
        public List<Double> precipitationSum;

        @JsonProperty("precipitation_probability_max")
        public List<Double> precipitationProbabilityMax;

        @JsonProperty("wind_speed_10m_max")
        public List<Double> windSpeed10mMax;
    }
}