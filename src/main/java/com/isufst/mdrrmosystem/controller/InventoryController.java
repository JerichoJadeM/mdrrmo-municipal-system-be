package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.InventoryRequest;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import com.isufst.mdrrmosystem.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public InventoryResponse create(@RequestBody InventoryRequest inventoryRequest){
        return inventoryService.create(inventoryRequest);
    }

    @GetMapping
    public List<InventoryResponse> getInventory(){
        return inventoryService.getAll();
    }
}
