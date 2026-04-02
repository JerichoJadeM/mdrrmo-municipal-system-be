package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.request.InventoryCreateProcurementRequest;
import com.isufst.mdrrmosystem.request.InventoryProcurementRequest;

public interface InventoryProcurementExecutorService {
    void executeProcurement(long inventoryId, InventoryProcurementRequest request, User actor);
    Inventory executeNewInventoryProcurement(InventoryCreateProcurementRequest request, User actor);
}
