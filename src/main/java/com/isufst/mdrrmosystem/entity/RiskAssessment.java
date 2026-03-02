package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "barangay_id")
    private Barangay barangay;

    @Column(name = "risk_level")
    private String riskLevel; // LOW , MEDIUM, HIGH

    private String reason;

    @Column(nullable = false, name = "assessed_at")
    private LocalDateTime assessedAt;

    public RiskAssessment() {}

    public RiskAssessment(Barangay barangay, String riskLevel, String reason, LocalDateTime assessedAt) {
        this.barangay = barangay;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.assessedAt = assessedAt;
    }

    public Barangay getBarangay() {
        return barangay;
    }

    public void setBarangay(Barangay barangay) {
        this.barangay = barangay;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }
}
