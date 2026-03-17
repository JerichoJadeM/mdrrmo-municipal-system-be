package com.isufst.mdrrmosystem.entity;

public class ForecastTemplateItem {

    private String itemName;
    private String category;
    private int quantity;
    private String unit;
    private String resourceType;
    private double estimatedUnitCost;
    private String reason;

    public ForecastTemplateItem() {
    }

    public ForecastTemplateItem(String itemName,
                                String category,
                                int quantity,
                                String unit,
                                String resourceType,
                                double estimatedUnitCost,
                                String reason) {
        this.itemName = itemName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.resourceType = resourceType;
        this.estimatedUnitCost = estimatedUnitCost;
        this.reason = reason;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getResourceType() {
        return resourceType;
    }

    public double getEstimatedUnitCost() {
        return estimatedUnitCost;
    }

    public String getReason() {
        return reason;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setEstimatedUnitCost(double estimatedUnitCost) {
        this.estimatedUnitCost = estimatedUnitCost;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}