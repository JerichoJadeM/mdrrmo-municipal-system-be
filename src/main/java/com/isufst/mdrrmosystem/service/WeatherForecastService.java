package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.external.weather.OpenMeteoWeatherProvider;
import com.isufst.mdrrmosystem.external.weather.PagasaWeatherProvider;
import com.isufst.mdrrmosystem.external.weather.dto.ExternalWeatherPayload;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.response.BarangayRiskForecastResponse;
import com.isufst.mdrrmosystem.response.DailyForecastResponse;
import com.isufst.mdrrmosystem.response.MunicipalWeatherForecastResponse;
import com.isufst.mdrrmosystem.response.WeatherSnapshotResponse;
import com.isufst.mdrrmosystem.response.WeatherSummaryResponse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class WeatherForecastService {

    private final BarangayRepository barangayRepository;
    private final IncidentRepository incidentRepository;
    private final PagasaWeatherProvider pagasaWeatherProvider;
    private final OpenMeteoWeatherProvider openMeteoWeatherProvider;

    public WeatherForecastService(BarangayRepository barangayRepository,
                                  IncidentRepository incidentRepository,
                                  PagasaWeatherProvider pagasaWeatherProvider,
                                  OpenMeteoWeatherProvider openMeteoWeatherProvider) {
        this.barangayRepository = barangayRepository;
        this.incidentRepository = incidentRepository;
        this.pagasaWeatherProvider = pagasaWeatherProvider;
        this.openMeteoWeatherProvider = openMeteoWeatherProvider;
    }

    public MunicipalWeatherForecastResponse getMunicipalForecast() {
        ExternalWeatherPayload payload = fetchFromBestSource();

        List<Barangay> barangays = barangayRepository.findAll();
        List<BarangayRiskForecastResponse> barangayRisks = buildBarangayRiskForecasts(barangays, payload.current());
        WeatherSummaryResponse summary = buildSummary(payload.current(), payload.dailyForecasts(), barangayRisks);

        return new MunicipalWeatherForecastResponse(
                "Batad",
                "Iloilo",
                payload.source(),
                LocalDateTime.now(),
                payload.current(),
                summary,
                payload.dailyForecasts(),
                barangayRisks,
                payload.alerts()
        );
    }

    private ExternalWeatherPayload fetchFromBestSource() {
        try {
            return pagasaWeatherProvider.fetchBatadWeather();
        } catch (Exception pagasaError) {
            return openMeteoWeatherProvider.fetchBatadWeather();
        }
    }

    private List<BarangayRiskForecastResponse> buildBarangayRiskForecasts(List<Barangay> barangays,
                                                                          WeatherSnapshotResponse current) {
        List<BarangayRiskForecastResponse> rows = new ArrayList<>();

        for (Barangay barangay : barangays) {
            long ongoingIncidents = incidentRepository.countByBarangayIdAndStatus(barangay.getId(), "ONGOING");
            RiskComputation risk = computeBarangayRisk(
                    barangay,
                    current.rainfall(),
                    current.windSpeed(),
                    current.condition(),
                    ongoingIncidents
            );

            rows.add(new BarangayRiskForecastResponse(
                    barangay.getId(),
                    barangay.getName(),
                    readBoolean(barangay, "isFloodProne"),
                    readBoolean(barangay, "isLandslideProne"),
                    readBoolean(barangay, "isCoastal"),
                    risk.level(),
                    risk.reason(),
                    risk.recommendation()
            ));
        }

        rows.sort(Comparator
                .comparingInt((BarangayRiskForecastResponse r) -> riskRank(r.riskLevel()))
                .thenComparing(BarangayRiskForecastResponse::barangayName));

        return rows;
    }

    private WeatherSummaryResponse buildSummary(WeatherSnapshotResponse current,
                                                List<DailyForecastResponse> forecasts,
                                                List<BarangayRiskForecastResponse> barangayRisks) {
        int total = barangayRisks.size();
        int high = (int) barangayRisks.stream().filter(r -> "HIGH".equalsIgnoreCase(r.riskLevel()) || "SEVERE".equalsIgnoreCase(r.riskLevel())).count();
        int medium = (int) barangayRisks.stream().filter(r -> "MEDIUM".equalsIgnoreCase(r.riskLevel()) || "MODERATE".equalsIgnoreCase(r.riskLevel())).count();
        int low = Math.max(0, total - high - medium);

        String overallRisk = deriveOverallRiskLevel(high, medium, total, current);
        String rainfallOutlook = deriveRainfallOutlook(current, forecasts);
        String recommendation = deriveMunicipalRecommendation(overallRisk, current);

        return new WeatherSummaryResponse(
                overallRisk,
                rainfallOutlook,
                recommendation,
                total,
                high,
                medium,
                low
        );
    }

    private RiskComputation computeBarangayRisk(Barangay barangay,
                                                double rainfall,
                                                double windSpeed,
                                                String condition,
                                                long ongoingIncidents) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        boolean floodProne = readBoolean(barangay, "isFloodProne");
        boolean landslideProne = readBoolean(barangay, "isLandslideProne");
        boolean coastal = readBoolean(barangay, "isCoastal");

        if (rainfall > 100) {
            score += 2;
            reasons.add("heavy rainfall");
        } else if (rainfall > 50) {
            score += 1;
            reasons.add("moderate rainfall");
        }

        if (floodProne && rainfall > 50) {
            score += 2;
            reasons.add("flood-prone area");
        }

        if (landslideProne && rainfall > 80) {
            score += 2;
            reasons.add("landslide-prone area");
        }

        if (coastal && windSpeed >= 25) {
            score += 1;
            reasons.add("coastal exposure");
        }

        if (safe(condition).toLowerCase().contains("thunder")) {
            score += 1;
            reasons.add("thunderstorm conditions");
        }

        if (ongoingIncidents >= 4) {
            score += 2;
            reasons.add("multiple ongoing incidents");
        } else if (ongoingIncidents >= 2) {
            score += 1;
            reasons.add("active incident load");
        }

        String level;
        if (score >= 5) level = "HIGH";
        else if (score >= 3) level = "MEDIUM";
        else level = "LOW";

        String reason = reasons.isEmpty() ? "Normal conditions" : String.join(", ", reasons);
        String recommendation = buildBarangayRecommendation(level, floodProne, landslideProne, coastal);

        return new RiskComputation(level, reason, recommendation);
    }

    private String buildBarangayRecommendation(String riskLevel,
                                               boolean floodProne,
                                               boolean landslideProne,
                                               boolean coastal) {
        if ("HIGH".equalsIgnoreCase(riskLevel)) {
            if (floodProne) return "Prepare evacuation support, monitor waterways, and preposition flood response resources.";
            if (landslideProne) return "Closely monitor slopes and road access, and prepare precautionary evacuation messaging.";
            if (coastal) return "Monitor coastal conditions and secure exposed assets and response staging points.";
            return "Raise readiness posture and prepare response teams and critical resources.";
        }

        if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
            return "Increase monitoring and review preparedness of responders, equipment, and vulnerable households.";
        }

        return "Continue normal monitoring and maintain basic preparedness.";
    }

    private String deriveOverallRiskLevel(int highRiskBarangays,
                                          int mediumRiskBarangays,
                                          int totalBarangays,
                                          WeatherSnapshotResponse current) {
        if (highRiskBarangays >= Math.max(1, totalBarangays / 3)) return "HIGH";
        if (highRiskBarangays > 0 || mediumRiskBarangays >= Math.max(2, totalBarangays / 4)) return "MEDIUM";
        if (current.rainfall() > 80) return "MEDIUM";
        return "LOW";
    }

    private String deriveRainfallOutlook(WeatherSnapshotResponse current,
                                         List<DailyForecastResponse> forecasts) {
        double peak = forecasts.stream()
                .map(DailyForecastResponse::rainfallMm)
                .filter(v -> v != null)
                .max(Double::compareTo)
                .orElse(current.rainfall());

        if (peak >= 100) return "Heavy rainfall likely";
        if (peak >= 50) return "Moderate rainfall expected";
        if (peak >= 20) return "Light to moderate rain possible";
        return "Generally light rainfall";
    }

    private String deriveMunicipalRecommendation(String overallRiskLevel,
                                                 WeatherSnapshotResponse current) {
        if ("HIGH".equalsIgnoreCase(overallRiskLevel)) {
            return "Prepare evacuation support, alert barangay responders, and preposition critical resources.";
        }
        if ("MEDIUM".equalsIgnoreCase(overallRiskLevel)) {
            return "Increase monitoring of vulnerable barangays and review response readiness.";
        }
        if (safe(current.condition()).toLowerCase().contains("thunder")) {
            return "Maintain normal monitoring but brief field responders on possible thunderstorm development.";
        }
        return "Continue routine monitoring and maintain municipal preparedness.";
    }

    private int riskRank(String riskLevel) {
        String v = safe(riskLevel).toUpperCase();
        return switch (v) {
            case "SEVERE" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM", "MODERATE" -> 2;
            default -> 3;
        };
    }

    private boolean readBoolean(Barangay barangay, String methodName) {
        try {
            Object value = barangay.getClass().getMethod(methodName).invoke(barangay);
            return value instanceof Boolean && (Boolean) value;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record RiskComputation(String level, String reason, String recommendation) {}

}