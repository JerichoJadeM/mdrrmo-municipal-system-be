package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String type;

    @ManyToOne
    @JoinColumn(name = "barangay_id", nullable = false)
    private Barangay barangay;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(length = 1000)
    private String description;

    @ManyToMany
    @JoinTable(
            name = "incident_assigned_responders",
            joinColumns = @JoinColumn(name = "incident_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> assignedResponders = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archive_cleared_at")
    private LocalDateTime archiveClearedAt;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    public Incident() {}

    public Incident(String type, Barangay barangay, String severity, String status, LocalDateTime reportedAt, String description) {
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

    public List<User> getAssignedResponders() {
        return assignedResponders;
    }

    public void setAssignedResponders(List<User> assignedResponders) {
        this.assignedResponders = assignedResponders;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public LocalDateTime getArchiveClearedAt() {
        return archiveClearedAt;
    }

    public void setArchiveClearedAt(LocalDateTime archiveClearedAt) {
        this.archiveClearedAt = archiveClearedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}