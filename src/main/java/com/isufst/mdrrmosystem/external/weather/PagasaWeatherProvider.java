package com.isufst.mdrrmosystem.external.weather;

import com.isufst.mdrrmosystem.external.weather.dto.ExternalWeatherPayload;
import com.isufst.mdrrmosystem.response.DailyForecastResponse;
import com.isufst.mdrrmosystem.response.WeatherAlertResponse;
import com.isufst.mdrrmosystem.response.WeatherSnapshotResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class PagasaWeatherProvider implements WeatherProvider {

    private static final String PAGASA_AWS_URL = "https://bagong.pagasa.dost.gov.ph/automated-weather-station/";
    private static final String PAGASA_TENDAY_BASE_URL = "https://tenday.pagasa.dost.gov.ph/api/v1";

    private final RestClient restClient;

    @Value("${pagasa.tenday.token:}")
    private String pagasaToken;

    public PagasaWeatherProvider() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public ExternalWeatherPayload fetchBatadWeather() {
        WeatherSnapshotResponse current = fetchCurrentFromAwsPage();
        List<DailyForecastResponse> dailyForecasts = fetchForecastFromTenDayApi();
        List<WeatherAlertResponse> alerts = buildAlerts(current, dailyForecasts, "PAGASA");

        return new ExternalWeatherPayload(
                "PAGASA",
                current,
                dailyForecasts,
                alerts
        );
    }

    private WeatherSnapshotResponse fetchCurrentFromAwsPage() {
        try {
            Document doc = Jsoup.connect(PAGASA_AWS_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Element row = findPreferredIloiloRow(doc);
            if (row == null) {
                throw new IllegalStateException("No suitable Iloilo PAGASA AWS station row found.");
            }

            Elements cells = row.select("td");
            if (cells.size() < 8) {
                throw new IllegalStateException("Unexpected PAGASA AWS table structure.");
            }

            double temperature = parseLeadingDouble(cells.get(1).text());
            double windSpeed = parseLeadingDouble(cells.get(3).text());
            double rainfall = parseLeadingDouble(cells.get(5).text());

            String stationName = cleanText(cells.get(0).text());
            String condition = rainfall > 15
                    ? "Rain showers near " + stationName
                    : "Partly cloudy near " + stationName;

            return new WeatherSnapshotResponse(
                    rainfall,
                    temperature,
                    windSpeed,
                    condition
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch PAGASA AWS page.", e);
        }
    }

    private Element findPreferredIloiloRow(Document doc) {
        Elements rows = doc.select("table tbody tr");
        for (Element row : rows) {
            String text = row.text().toLowerCase();
            if (text.contains("san enrique, iloilo aws")) return row;
        }
        for (Element row : rows) {
            String text = row.text().toLowerCase();
            if (text.contains("dumangas, iloilo aws")) return row;
        }
        for (Element row : rows) {
            String text = row.text().toLowerCase();
            if (text.contains("janiuay iloilo aws")) return row;
        }
        return null;
    }

    private List<DailyForecastResponse> fetchForecastFromTenDayApi() {
        if (pagasaToken == null || pagasaToken.isBlank()) {
            throw new IllegalStateException("PAGASA TenDay token not configured.");
        }

        /*
         * Adjust this endpoint path and mapping to your approved PAGASA account response format.
         * Different PAGASA datasets may have slightly different payload structures.
         *
         * Example pattern only:
         * GET /api/v1/forecast?province=Iloilo&municipality=Batad
         */
        PagasaTenDayResponse response = restClient.get()
                .uri(PAGASA_TENDAY_BASE_URL + "/forecast?province=Iloilo&municipality=Batad")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pagasaToken)
                .retrieve()
                .body(PagasaTenDayResponse.class);

        if (response == null || response.forecasts() == null || response.forecasts().isEmpty()) {
            throw new IllegalStateException("PAGASA TenDay returned no forecast rows.");
        }

        List<DailyForecastResponse> rows = new ArrayList<>();
        for (PagasaTenDayDay day : response.forecasts()) {
            rows.add(new DailyForecastResponse(
                    day.date(),
                    safe(day.condition(), "Forecast unavailable"),
                    day.minTemperature(),
                    day.maxTemperature(),
                    day.rainfallMm(),
                    day.rainProbability(),
                    day.windSpeed(),
                    deriveRisk(day.rainfallMm(), day.windSpeed(), day.condition()),
                    deriveAdvisory(day.rainfallMm(), day.windSpeed(), day.condition())
            ));
        }
        return rows;
    }

    private List<WeatherAlertResponse> buildAlerts(WeatherSnapshotResponse current,
                                                   List<DailyForecastResponse> forecasts,
                                                   String source) {
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
                    "Possible moderate to heavy rainfall may affect low-lying and flood-prone areas.",
                    LocalDateTime.now(),
                    source
            ));
        }

        boolean hasThunder = forecasts.stream()
                .anyMatch(f -> safe(f.condition(), "").toLowerCase().contains("thunder"));

        if (hasThunder) {
            alerts.add(new WeatherAlertResponse(
                    "Thunderstorm Advisory",
                    "MODERATE",
                    "Whole Municipality",
                    "Thunderstorm activity is possible. Outdoor operations should monitor lightning and sudden wind changes.",
                    LocalDateTime.now(),
                    source
            ));
        }

        if (alerts.isEmpty()) {
            alerts.add(new WeatherAlertResponse(
                    "General Weather Monitoring",
                    "LOW",
                    "Whole Municipality",
                    "No significant weather alerts at this time. Continue routine monitoring.",
                    LocalDateTime.now(),
                    source
            ));
        }

        return alerts;
    }

    private String deriveRisk(Double rainfall, Double windSpeed, String condition) {
        double rain = rainfall == null ? 0 : rainfall;
        double wind = windSpeed == null ? 0 : windSpeed;
        String c = safe(condition, "").toLowerCase();

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

    private double parseLeadingDouble(String raw) {
        if (raw == null) return 0;
        String cleaned = raw.replaceAll("[^0-9.\\-]", " ").trim();
        if (cleaned.isBlank()) return 0;
        String first = cleaned.split("\\s+")[0];
        try {
            return Double.parseDouble(first);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record PagasaTenDayResponse(List<PagasaTenDayDay> forecasts) {}
    public record PagasaTenDayDay(
            LocalDate date,
            String condition,
            Double minTemperature,
            Double maxTemperature,
            Double rainfallMm,
            Double rainProbability,
            Double windSpeed
    ) {}
}
