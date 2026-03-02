package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "calamities")
public class Calamity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    // TODO: Convert 'type' and 'severity' to Enum once system stabilizes
    @Column(nullable = false)
    private String type; //Flood, Typhoon, Earthquake, Fire


    @ManyToOne
    @JoinColumn(name = "barangay_id")
    private  Barangay barangay;

    @Column(nullable = false)
    private  String severity; // LOW, MEDIUM, HIGH

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, name = "damage_cost")
    private double damageCost;

    @Column(nullable = false)
    private int casualties;

    @Column(length = 1000, nullable = false)
    private String description;

    public Calamity() {}

    public Calamity(String type, Barangay barangay, String severity, LocalDate date, double damageCost, int casualties, String description) {
        this.type = type;
        this.barangay = barangay;
        this.severity = severity;
        this.date = date;
        this.damageCost = damageCost;
        this.casualties = casualties;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Barangay getBarangay() {
        return barangay;
    }

    public void setBarangay(Barangay barangay) {
        this.barangay = barangay;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getDamageCost() {
        return damageCost;
    }

    public void setDamageCost(double damageCost) {
        this.damageCost = damageCost;
    }

    public int getCasualties() {
        return casualties;
    }

    public void setCasualties(int casualties) {
        this.casualties = casualties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
