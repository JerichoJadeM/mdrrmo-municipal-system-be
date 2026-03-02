package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryTransactionService {

    private InventoryRepository inventoryRepository;
    private InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryTransactionService(InventoryTransactionRepository inventoryTransactionRepository, InventoryRepository inventoryRepository) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryRepository = inventoryRepository;
    }


    public void deployItem(long inventoryId, long quantity) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow();

        if(inventory.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Not enough inventory");
        }

        inventory.setAvailableQuantity((int) (inventory.getAvailableQuantity() - quantity));

        inventoryRepository.save(inventory);
    }

}
