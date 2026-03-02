package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.request.InventoryRequest;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request){
        Inventory inventory = new Inventory();

        inventory.setName(request.name());
        inventory.setCategory(request.category());
        inventory.setTotalQuantity(request.totalQuantity());
        inventory.setAvailableQuantity(request.totalQuantity());
        inventory.setUnit(request.unit());
        inventory.setLocation(request.location());

        inventoryRepository.save(inventory);

        return mapToResponse(inventory);
    }

    public List<InventoryResponse> getAll(){
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InventoryResponse mapToResponse(Inventory inventory) {

        return new InventoryResponse(
                inventory.getId(),
                inventory.getName(),
                inventory.getCategory(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getUnit(),
                inventory.getLocation()
        );
    }
}
