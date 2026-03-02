package com.isufst.mdrrmosystem.request;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.time.LocalDate;

public class CalamityRequest {

    public String type;
    public Barangay barangay;
    public String severity;
    public LocalDate date;
    public Double damageCost;
    public Integer casualties;
    public String description;
}
