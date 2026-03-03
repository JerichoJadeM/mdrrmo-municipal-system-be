package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "action_type")
    private String actionType; // DEPLOY, RETURN, CONSUMED, DAMAGED

    private int quantity;

    @Column(name = "time_stamp")
    private LocalDateTime timeStamp;

    @ManyToOne
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne
    @JoinColumn(name = "response_action_id")
    private ResponseAction responseAction;

    @ManyToOne
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @ManyToOne
    @JoinColumn(name = "distribution_id")
    private ReliefDistribution distribution;

    public ReliefDistribution getDistribution() {
        return distribution;
    }

    public void setDistribution(ReliefDistribution distribution) {
        this.distribution = distribution;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }

    public ResponseAction getResponseAction() {
        return responseAction;
    }

    public void setResponseAction(ResponseAction responseAction) {
        this.responseAction = responseAction;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }
}
