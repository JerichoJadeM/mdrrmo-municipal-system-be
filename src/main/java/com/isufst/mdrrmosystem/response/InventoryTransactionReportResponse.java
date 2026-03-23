package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record InventoryTransactionReportResponse(
        Long id,
        Long inventoryId,
        String itemName,
        String actionType,
        Integer quantity,
        String performedBy,
        LocalDateTime timeStamp
) {
}
