package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.request.InventoryRequest;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        Inventory inventory = new Inventory();
        mapRequestToEntity(inventory, request, true);
        inventoryRepository.save(inventory);
        return mapToResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getResourcesView(String keyword, String category, String stockStatus) {
        return inventoryRepository.findForResourcesView(keyword, category)
                .stream()
                .map(this::mapToResponse)
                .filter(i -> stockStatus == null || stockStatus.isBlank() || stockStatus.equalsIgnoreCase(i.stockStatus()))
                .toList();
    }

    @Transactional
    public InventoryResponse update(long inventoryId, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found: " + inventoryId));

        int oldTotal = inventory.getTotalQuantity();
        int oldAvailable = inventory.getAvailableQuantity();

        mapRequestToEntity(inventory, request, false);

        if (request.totalQuantity() != oldTotal) {
            int delta = request.totalQuantity() - oldTotal;
            int newAvailable = oldAvailable + delta;
            if (newAvailable < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Total quantity cannot be reduced below deployed/consumed stock.");
            }
            inventory.setAvailableQuantity(newAvailable);
        }

        inventoryRepository.save(inventory);
        return mapToResponse(inventory);
    }

    private void mapRequestToEntity(Inventory inventory, InventoryRequest request, boolean createMode) {
        inventory.setName(request.name());
        inventory.setCategory(request.category());
        inventory.setTotalQuantity(request.totalQuantity());
        inventory.setUnit(request.unit());
        inventory.setLocation(request.location());
        inventory.setReorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 0);
        inventory.setCriticalItem(request.criticalItem() != null ? request.criticalItem() : Boolean.FALSE);

        if (createMode) {
            inventory.setAvailableQuantity(request.totalQuantity());
        }
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getName(),
                inventory.getCategory(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getUnit(),
                inventory.getLocation(),
                inventory.getReorderLevel(),
                inventory.getCriticalItem() != null ? inventory.getCriticalItem() : Boolean.FALSE,
                deriveStockStatus(inventory)
        );
    }

    private String deriveStockStatus(Inventory inventory) {
        if (inventory.getAvailableQuantity() <= 0) {
            return "OUT";
        }

        int reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0;
        if (inventory.getAvailableQuantity() <= reorderLevel) {
            return "LOW";
        }

        return "OK";
    }
}