package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.OperationResourceUsage;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.OperationResourceUsageRepository;
import com.isufst.mdrrmosystem.request.ResourceUsageItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persists the resources selected during an incident/calamity status transition and computes
 * the authoritative line cost server-side from inventory pricing. Any client-supplied unit
 * cost/total is never trusted for the persisted amount (OWASP: never trust client-supplied
 * financial data) - it is recomputed here from {@link Inventory#getEstimatedUnitCost()}.
 */
@Service
public class OperationResourceUsageService {

    private final OperationResourceUsageRepository operationResourceUsageRepository;
    private final InventoryRepository inventoryRepository;

    public OperationResourceUsageService(OperationResourceUsageRepository operationResourceUsageRepository,
                                         InventoryRepository inventoryRepository) {
        this.operationResourceUsageRepository = operationResourceUsageRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public double recordUsage(String operationType,
                              Long operationId,
                              String transitionMode,
                              List<ResourceUsageItemRequest> selectedResources,
                              User actor) {
        if (selectedResources == null || selectedResources.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (ResourceUsageItemRequest item : selectedResources) {
            if (item == null) {
                continue;
            }

            int quantity = item.quantity() != null ? item.quantity() : 0;
            if (quantity <= 0) {
                continue;
            }

            Inventory inventory = resolveInventory(item);
            double unitCost = inventory != null && inventory.getEstimatedUnitCost() != null
                    ? inventory.getEstimatedUnitCost()
                    : 0.0;
            String resolvedName = inventory != null ? inventory.getName() : item.itemName();
            double lineTotal = round2(unitCost * quantity);
            total += lineTotal;

            OperationResourceUsage usage = new OperationResourceUsage();
            usage.setOperationType(operationType);
            usage.setOperationId(operationId);
            usage.setTransitionMode(transitionMode);
            usage.setInventory(inventory);
            usage.setItemName(resolvedName);
            usage.setQuantity(quantity);
            usage.setUnitCost(unitCost);
            usage.setLineTotal(lineTotal);
            usage.setRecordedAt(LocalDateTime.now());
            usage.setRecordedBy(actor);
            operationResourceUsageRepository.save(usage);
        }

        return round2(total);
    }

    @Transactional(readOnly = true)
    public double getActualCostToDate(String operationType, Long operationId) {
        Double sum = operationResourceUsageRepository.sumLineTotalByOperationTypeAndOperationId(operationType, operationId);
        return sum != null ? round2(sum) : 0.0;
    }

    private Inventory resolveInventory(ResourceUsageItemRequest item) {
        if (item.inventoryId() != null) {
            Inventory byId = inventoryRepository.findById(item.inventoryId()).orElse(null);
            if (byId != null) {
                return byId;
            }
        }
        if (item.itemName() != null && !item.itemName().isBlank()) {
            return inventoryRepository.findFirstByNameIgnoreCase(item.itemName().trim()).orElse(null);
        }
        return null;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
