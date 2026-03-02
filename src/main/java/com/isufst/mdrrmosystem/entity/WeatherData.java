package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, name = "recorded_at")
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private double rainfall;

    @Column(nullable = false)
    private double temperature;

    @Column(nullable = false, name = "wind_speed")
    private double windSpeed;

    @Column(name = "weather_condition")
    private String condition;

    public WeatherData() {}

    public WeatherData(LocalDateTime recordedAt, double rainfall, double temperature, double windSpeed, String condition) {
        this.recordedAt = recordedAt;
        this.rainfall = rainfall;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.condition = condition;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public double getRainfall() {
        return rainfall;
    }

    public void setRainfall(double rainfall) {
        this.rainfall = rainfall;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
