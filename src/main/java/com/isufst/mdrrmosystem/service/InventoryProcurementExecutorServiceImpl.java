package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.InventoryCreateProcurementRequest;
import com.isufst.mdrrmosystem.request.InventoryProcurementRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class InventoryProcurementExecutorServiceImpl implements InventoryProcurementExecutorService {

    private final InventoryRepository inventoryRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final ExpenseRepository expenseRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryProcurementExecutorServiceImpl(InventoryRepository inventoryRepository,
                                                   BudgetCategoryRepository budgetCategoryRepository,
                                                   IncidentRepository incidentRepository,
                                                   CalamityRepository calamityRepository,
                                                   ExpenseRepository expenseRepository,
                                                   InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.expenseRepository = expenseRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    @Transactional
    public void executeProcurement(long inventoryId, InventoryProcurementRequest request, User actor) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        BudgetCategory category = budgetCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Budget category not found"));

        Incident incident = null;
        if (request.incidentId() != null) {
            incident = incidentRepository.findById(request.incidentId())
                    .orElseThrow(() -> new RuntimeException("Incident not found"));
        }

        Calamity calamity = null;
        if (request.calamityId() != null) {
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
        expense.setCreatedBy(actor);
        expenseRepository.save(expense);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setActionType("RESTOCK");
        transaction.setQuantity(request.quantityAdded());
        transaction.setTimeStamp(LocalDateTime.now());
        transaction.setInventory(inventory);
        transaction.setIncident(incident);
        transaction.setPerformedBy(actor);
        inventoryTransactionRepository.save(transaction);

        inventory.setProcurementExpense(expense);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory executeNewInventoryProcurement(InventoryCreateProcurementRequest request, User actor) {
        Inventory inventory = new Inventory();
        inventory.setName(request.name().trim());
        inventory.setCategory(request.category().trim());
        inventory.setUnit(request.unit().trim());
        inventory.setLocation(request.location().trim());
        inventory.setReorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 0);
        inventory.setCriticalItem(Boolean.TRUE.equals(request.criticalItem()));
        inventory.setEstimatedUnitCost(request.estimatedUnitCost());
        inventory.setCostLastUpdated(request.estimatedUnitCost() != null ? LocalDate.now() : null);
        inventory.setTotalQuantity(0);
        inventory.setAvailableQuantity(0);

        inventoryRepository.save(inventory);

        InventoryProcurementRequest procurementRequest = new InventoryProcurementRequest(
                request.categoryId(),
                request.quantityAdded(),
                request.totalCost(),
                request.expenseDate(),
                request.description(),
                request.incidentId(),
                request.calamityId()
        );

        executeProcurement(inventory.getId(), procurementRequest, actor);

        return inventoryRepository.findById(inventory.getId())
                .orElseThrow(() -> new RuntimeException("Inventory not found after procurement"));
    }
}
