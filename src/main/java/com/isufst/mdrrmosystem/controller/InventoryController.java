package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.InventoryAdjustmentRequest;
import com.isufst.mdrrmosystem.request.InventoryProcurementRequest;
import com.isufst.mdrrmosystem.request.InventoryRequest;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import com.isufst.mdrrmosystem.service.InventoryService;
import com.isufst.mdrrmosystem.service.InventoryTransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;
    private final InventoryTransactionService inventoryTransactionService;

    public InventoryController(InventoryService inventoryService,
                               InventoryTransactionService inventoryTransactionService) {
        this.inventoryService = inventoryService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @PostMapping
    public InventoryResponse create(@RequestBody InventoryRequest inventoryRequest) {
        return inventoryService.create(inventoryRequest);
    }

    @GetMapping
    public List<InventoryResponse> getInventory() {
        return inventoryService.getAll();
    }

    @GetMapping("/resources-view")
    public List<InventoryResponse> getResourcesView(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String stockStatus) {
        return inventoryService.getResourcesView(keyword, category, stockStatus);
    }

    @PutMapping("/{id}")
    public InventoryResponse update(@PathVariable long id,
                                    @RequestBody InventoryRequest inventoryRequest) {
        return inventoryService.update(id, inventoryRequest);
    }

    @PatchMapping("/{id}/adjust-stock")
    public InventoryResponse adjustStock(@PathVariable long id,
                                         @RequestBody InventoryAdjustmentRequest request) {
        return inventoryTransactionService.adjustStock(id, request);
    }

    @PatchMapping("/{inventoryId}/procure")
    public InventoryResponse procureStock(@PathVariable long inventoryId,
                                          @RequestBody InventoryProcurementRequest request) {
        return inventoryService.procureStock(inventoryId, request);
    }
}
