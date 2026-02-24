package com.isufst.mdrrmosystem.response;

public class CategoryBreakdownResponse {

    private final String name;
    private final double allocatedAmount;

    public CategoryBreakdownResponse(String name, double allocatedAmount) {
        this.name = name;
        this.allocatedAmount = allocatedAmount;
    }

    public String getName() {
        return name;
    }

    public double getAllocatedAmount() {
        return allocatedAmount;
    }

}
