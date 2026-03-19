package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;

    @Column(nullable = false)
    private String category;// VEHICLE, RESCUE EQUIPMENT, MEDICAL, FOOD, TOOL

    @Column(name = "total_quantity")
    private int totalQuantity;

    @Column(name = "available_quantity")
    private int availableQuantity;

    @Column(nullable = false)
    private String unit; // pcs, boxes, liters, kits

    @Column(nullable = false)
    private String location; // MDRRMO warehouse, firestation, barangay storage

    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @Column(name = "is_critical_item")
    private Boolean criticalItem;

    @ManyToOne
    @JoinColumn(name = "procurement_expense_id")
    private Expense procurementExpense;

    public Expense getProcurementExpense() {
        return procurementExpense;
    }

    public void setProcurementExpense(Expense procurementExpense) {
        this.procurementExpense = procurementExpense;
    }

    public Inventory() {}

    public Inventory(String name, String category, int totalQuantity, int availableQuantity, String unit, String location) {
        this.name = name;
        this.category = category;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.unit = unit;
        this.location = location;
    }

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Boolean getCriticalItem() {
        return criticalItem;
    }

    public void setCriticalItem(Boolean criticalItem) {
        this.criticalItem = criticalItem;
    }
}
