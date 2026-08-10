package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "previous_budget",
        uniqueConstraints = @UniqueConstraint(name = "uk_previous_budget_year", columnNames = "year")
)
public class PreviousBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Double allotment;

    private Double obligations;

    private Double remaining;

    @Column(name="utilization_rate")
    private Double utilizationRate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }
    public PreviousBudget() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getAllotment() {
        return allotment;
    }

    public void setAllotment(Double allotment) {
        this.allotment = allotment;
    }

    public Double getObligations() {
        return obligations;
    }

    public void setObligations(Double obligations) {
        this.obligations = obligations;
    }

    public Double getRemaining() {
        return remaining;
    }

    public void setRemaining(Double remaining) {
        this.remaining = remaining;
    }

    public Double getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(Double utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
