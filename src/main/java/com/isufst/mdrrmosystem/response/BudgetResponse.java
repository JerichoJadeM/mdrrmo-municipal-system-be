package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.User;

import java.util.List;

public class BudgetResponse {

    private long id;
    private int year;
    private double totalAmount;
    private String description;
    private List<CategoryResponse> categories;
    private String createdBy;

    public BudgetResponse(long id, int year, double totalAmount, String description, List<BudgetCategory> categories, String createdBy) {
        this.id = id;
        this.year = year;
        this.totalAmount = totalAmount;
        this.description = description;
        this.categories = categories.stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getAllocatedAmount()))
                .toList();
        this.createdBy = createdBy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<CategoryResponse> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryResponse> categories) {
        this.categories = categories;
    }
}
