package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "relief_pack_template_items")
public class ReliefPackTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ReliefPackTemplate template;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Column(name = "quantity_required", nullable = false)
    private int quantityRequired;

    public Long getId() {
        return id;
    }

    public ReliefPackTemplate getTemplate() {
        return template;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getQuantityRequired() {
        return quantityRequired;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTemplate(ReliefPackTemplate template) {
        this.template = template;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setQuantityRequired(int quantityRequired) {
        this.quantityRequired = quantityRequired;
    }

}
