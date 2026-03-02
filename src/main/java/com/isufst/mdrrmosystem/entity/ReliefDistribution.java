package com.isufst.mdrrmosystem.entity;



import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "relief_distribution")
public class ReliefDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_type")
    private String itemType;
    // e.g. FOOD_PACK, WATER, MEDICINE, HYGIENE_KIT

    private int quantity;

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;

    @ManyToOne
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne
    @JoinColumn(name = "evacuation_activation_id")
    private EvacuationActivation evacuationActivation;

    @ManyToOne
    @JoinColumn(name = "distributed_by")
    private User distributedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getDistributedAt() {
        return distributedAt;
    }

    public void setDistributedAt(LocalDateTime distributedAt) {
        this.distributedAt = distributedAt;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public EvacuationActivation getEvacuationActivation() {
        return evacuationActivation;
    }

    public void setEvacuationActivation(EvacuationActivation evacuationActivation) {
        this.evacuationActivation = evacuationActivation;
    }

    public User getDistributedBy() {
        return distributedBy;
    }

    public void setDistributedBy(User distributedBy) {
        this.distributedBy = distributedBy;
    }
}
