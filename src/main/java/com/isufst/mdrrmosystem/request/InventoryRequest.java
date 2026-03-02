package com.isufst.mdrrmosystem.request;

public record InventoryRequest(
        String name,
        String category,
        int totalQuantity,
        String unit,
        String location
) {
}
