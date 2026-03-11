package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calamities")
public class Calamity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    // TODO: Convert 'type' and 'severity' to Enum once system stabilizes
    @Column(nullable = false)
    private String type;

    @Column(name = "event_name")
    private String eventName;

    @Column(nullable = false)
    private String status; // ACTIVE, MONITORING, RESOLVED, ENDED

    @Column(name = "affected_area_type", nullable = false)
    private String affectedAreaTypes; // one barangay, multiple barangays, municipality

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "barangay_id", nullable = true)
    private Barangay barangay;

    @ManyToMany
    @JoinTable(
            name = "calamity_affected_barangays",
            joinColumns = @JoinColumn(name = "calamity_id"),
            inverseJoinColumns = @JoinColumn(name = "barangay_id")
    )
    private List<Barangay> affectedBarangays = new ArrayList<>();

    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, name = "damage_cost")
    private BigDecimal damageCost;

    @Column(nullable = false)
    private int casualties;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @Column(length = 1000, nullable = false)
    private String description;

    public Calamity() {}

    public Calamity(String type, Barangay barangay, String severity, LocalDate date, BigDecimal damageCost, int casualties, String description) {
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


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public BigDecimal getDamageCost() {
        return damageCost;
    }

    public void setDamageCost(BigDecimal damageCost) {
        this.damageCost = damageCost;
    }

    public int getCasualties() {
        return casualties;
    }

    public void setCasualties(int casualties) {
        this.casualties = casualties;
    }

    public User getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(User coordinator) {
        this.coordinator = coordinator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getAffectedAreaTypes() {
        return affectedAreaTypes;
    }

    public void setAffectedAreaTypes(String affectedAreaTypes) {
        this.affectedAreaTypes = affectedAreaTypes;
    }

    public List<Barangay> getAffectedBarangays() {
        return affectedBarangays;
    }

    public void setAffectedBarangays(List<Barangay> affectedBarangays) {
        this.affectedBarangays = affectedBarangays;
    }
}