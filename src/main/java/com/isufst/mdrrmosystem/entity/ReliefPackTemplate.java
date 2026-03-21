package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "relief_pack_templates")
public class ReliefPackTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Food Pack - Plastic, Food Pack - Box

    @Column(name = "pack_type", nullable = false)
    private String packType; // PLASTIC, BOX, CUSTOM

    @Column(name = "intended_use", nullable = false)
    private String intendedUse; // EVACUEE, FAMILY, FIRE_VICTIM, GENERAL_RELIEF

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReliefPackTemplateItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPackType() {
        return packType;
    }

    public String getIntendedUse() {
        return intendedUse;
    }

    public boolean isActive() {
        return active;
    }

    public List<ReliefPackTemplateItem> getItems() {
        return items;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPackType(String packType) {
        this.packType = packType;
    }

    public void setIntendedUse(String intendedUse) {
        this.intendedUse = intendedUse;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setItems(List<ReliefPackTemplateItem> items) {
        this.items = items;
    }
}
