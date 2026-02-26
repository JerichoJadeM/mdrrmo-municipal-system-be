package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String type; // Vehicular Accident, Fire, Flood, Medical Emergency

    private String barangay;
    private String severity; // Low, Medium, High
    private String status; // Ongoing, Resolved

    @Column(nullable = false, name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    public Incident() {}

    public Incident(String type, String barangay, String severity, String status, LocalDateTime reportedAt, String description) {
        this.type = type;
        this.barangay = barangay;
        this.severity = severity;
        this.status = status;
        this.reportedAt = reportedAt;
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

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
    }
}
