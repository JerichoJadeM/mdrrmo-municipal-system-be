package com.isufst.mdrrmosystem.request;

import jakarta.validation.constraints.*;

public class BudgetRequest {

    @NotNull(message = "Year is mandatory")
    @Min(value = 1000, message = "Year must be 4 digits")
    @Max(value = 9999, message = "Year must be 4 digits")
    private int year;

    @NotNull(message = "Amount is mandatory")
    @Positive(message = "Amount must be positive")
    private double totalAmount;

    @NotBlank(message = "Description is mandatory")
    private String description;

    public BudgetRequest(int year, double totalAmount, String description) {
        this.year = year;
        this.totalAmount = totalAmount;
        this.description = description;
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
}