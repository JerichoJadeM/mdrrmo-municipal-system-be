package com.isufst.mdrrmosystem.request;

public record ReliefPackTemplateItemRequest(
        Long inventoryId,
        Integer quantityRequired
) {
}
