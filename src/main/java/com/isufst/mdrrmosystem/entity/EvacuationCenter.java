package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evacuation_center")
public class EvacuationCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; //e.g San Isidro Gym
    private String name;

    @ManyToOne
    @JoinColumn(name = "barangay_id")
    private Barangay barangay;
    private int capacity;

    @Column(name = "location_details")
    private String locationDetails;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Barangay getBarangay() {
        return barangay;
    }

    public void setBarangay(Barangay barangay) {
        this.barangay = barangay;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getLocationDetails() {
        return locationDetails;
    }

    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }
}
