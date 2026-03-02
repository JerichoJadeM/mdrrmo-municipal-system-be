package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "barangay")
public class Barangay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int population;

    @Column(name = "flood_prone")
    private boolean floodProne;

    @Column(name = "landslide_prone")
    private boolean landslideProne;
    private boolean coastal;

    @Column(name = "risk_level")
    private String riskLevel;

    public Barangay() {}

    public Barangay(String name, int population,  boolean floodProne, boolean landslideProne, boolean coastal) {
        this.name = name;
        this.population = population;
        this.floodProne = floodProne;
        this.landslideProne = landslideProne;
        this.coastal = coastal;
    }

    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public boolean isFloodProne() {
        return floodProne;
    }

    public void setFloodProne(boolean floodProne) {
        this.floodProne = floodProne;
    }

    public boolean isLandslideProne() {
        return landslideProne;
    }

    public void setLandslideProne(boolean landslideProne) {
        this.landslideProne = landslideProne;
    }

    public boolean isCoastal() {
        return coastal;
    }

    public void setCoastal(boolean coastal) {
        this.coastal = coastal;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
