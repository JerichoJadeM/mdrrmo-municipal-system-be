package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "barangay")
public class Barangay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "psgc_code")
    private String psgcCode;

    @Column(nullable = false, name = "municipality_name")
    private String municipalityName;

    @Column(nullable = false, name = "province_name")
    private String provinceName;

    @Column(nullable = false, name = "is_active")
    private Boolean active = true;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int population;

    @Column(name = "flood_prone", columnDefinition = "TINYINT(1) default 0")
    private boolean floodProne;

    @Column(name = "landslide_prone", columnDefinition = "TINYINT(1) default 0")
    private boolean landslideProne;

    @Column(name = "coastal", columnDefinition = "TINYINT(1) default 0")
    private boolean coastal;

    @Column(name = "risk_level")
    private String riskLevel;

    public Barangay() {}

    public Barangay(String name, int population, boolean floodProne, boolean landslideProne, boolean coastal) {
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

    public String getPsgcCode() {
        return psgcCode;
    }

    public void setPsgcCode(String psgcCode) {
        this.psgcCode = psgcCode;
    }

    public String getMunicipalityName() {
        return municipalityName;
    }

    public void setMunicipalityName(String municipalityName) {
        this.municipalityName = municipalityName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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