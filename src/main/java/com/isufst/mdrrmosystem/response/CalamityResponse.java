package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.time.LocalDate;

public class CalamityResponse {

    public Long id;
    public String type;
    public Barangay barangay;
    public String severity;
    public LocalDate date;
    public Double damageCost;
    public Integer casualties;
    public String description;

    public CalamityResponse(Long id, String type, Barangay barangay, String severity, LocalDate date, Double damageCost, Integer casualties, String description) {
        this.id = id;
        this.type = type;
        this.barangay = barangay;
        this.severity = severity;
        this.date = date;
        this.damageCost = damageCost;
        this.casualties = casualties;
        this.description = description;
    }
}
