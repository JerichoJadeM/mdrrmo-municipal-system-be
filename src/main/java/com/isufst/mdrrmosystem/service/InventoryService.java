package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.InventoryProcurementRequest;
import com.isufst.mdrrmosystem.request.InventoryRequest;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ExpenseRepository expenseRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            BudgetCategoryRepository budgetCategoryRepository,
                            IncidentRepository incidentRepository,
                            CalamityRepository calamityRepository,
                            FindAuthenticatedUser findAuthenticatedUser,
                            ExpenseRepository expenseRepository,
                            InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.expenseRepository = expenseRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
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
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setName(request.name());
        inventory.setCategory(request.category());
        inventory.setUnit(request.unit());
        inventory.setLocation(request.location());
        inventory.setReorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 0);
        inventory.setCriticalItem(Boolean.TRUE.equals(request.criticalItem()));

        if (request.totalQuantity() >= 0) {
            int difference = request.totalQuantity() - inventory.getTotalQuantity();
            inventory.setTotalQuantity(request.totalQuantity());
            inventory.setAvailableQuantity(Math.max(0, inventory.getAvailableQuantity() + difference));
        }

        inventory.setEstimatedUnitCost(request.estimatedUnitCost());
        inventory.setCostLastUpdated(request.estimatedUnitCost() != null ? LocalDate.now() : null);

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
                deriveStockStatus(inventory),
                inventory.getEstimatedUnitCost()
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

    @Transactional
    public InventoryResponse procureStock(long inventoryId, InventoryProcurementRequest request) {

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (request.incidentId() != null && request.calamityId() != null) {
            throw new RuntimeException("Procurement can only be linked to one operation");
        }

        if (request.quantityAdded() == null || request.quantityAdded() <= 0) {
            throw new RuntimeException("Quantity added must be greater than 0");
        }

        if (request.totalCost() == null || request.totalCost() <= 0) {
            throw new RuntimeException("Total cost must be greater than 0");
        }

        BudgetCategory category = budgetCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Budget category not found"));

        Incident incident = null;
        if (request.incidentId() != null) {
            incident = incidentRepository.findById(request.incidentId())
                    .orElseThrow(() -> new RuntimeException("Incident not found"));
        }

        Calamity calamity = null;
        if(request.calamityId() != null) {
            calamity = calamityRepository.findById(request.calamityId())
                    .orElseThrow(() -> new RuntimeException("Calamity not found"));
        }

        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.quantityAdded());
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.quantityAdded());
        inventoryRepository.save(inventory);

        Expense expense = new Expense();
        expense.setDescription(request.description() != null && !request.description().isBlank()
                ? request.description()
                : "Procurement / replenishment for " + inventory.getName());
        expense.setAmount(request.totalCost());
        expense.setExpenseDate(request.expenseDate() != null ? request.expenseDate() : LocalDate.now());
        expense.setCategory(category);
        expense.setIncident(incident);
        expense.setCalamity(calamity);
        expense.setCreatedBy(findAuthenticatedUser.getAuthenticatedUser());
        expenseRepository.save(expense);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setActionType("RESTOCK");
        transaction.setQuantity(request.quantityAdded());
        transaction.setTimeStamp(LocalDateTime.now());
        transaction.setInventory(inventory);
        transaction.setIncident(incident);
        transaction.setPerformedBy(findAuthenticatedUser.getAuthenticatedUser());
        inventoryTransactionRepository.save(transaction);

        inventory.setProcurementExpense(expense);
        inventoryRepository.save(inventory);

        return mapToResponse(inventory);
    }
}