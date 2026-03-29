package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.request.InventoryProcurementRequest;
import com.isufst.mdrrmosystem.request.InventoryRequest;
import com.isufst.mdrrmosystem.response.ActionSubmissionResponse;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
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
    private final NotificationService notificationService;
    private final ApprovalRequestService approvalRequestService;
    private final InventoryProcurementExecutorService inventoryProcurementExecutorService;

    public InventoryService(InventoryRepository inventoryRepository,
                            BudgetCategoryRepository budgetCategoryRepository,
                            IncidentRepository incidentRepository,
                            CalamityRepository calamityRepository,
                            FindAuthenticatedUser findAuthenticatedUser,
                            ExpenseRepository expenseRepository,
                            InventoryTransactionRepository inventoryTransactionRepository,
                            NotificationService notificationService,
                            ApprovalRequestService  approvalRequestService,
                            InventoryProcurementExecutorService inventoryProcurementExecutorService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.expenseRepository = expenseRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.notificationService = notificationService;
        this.approvalRequestService = approvalRequestService;
        this.inventoryProcurementExecutorService = inventoryProcurementExecutorService;
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
    public ActionSubmissionResponse procureStock(long inventoryId, InventoryProcurementRequest request) {
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

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        boolean isElevated = actor.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role));

        if (!isElevated) {
            ApprovalRequestResponse created = approvalRequestService.createRequest(
                    new ApprovalRequestCreateRequest(
                            "PROCUREMENT_REQUEST",
                            "Procurement request for " + inventory.getName(),
                            "Request to procure " + request.quantityAdded() + " " + inventory.getUnit() + " of " + inventory.getName(),
                            "INVENTORY",
                            inventory.getId(),
                            buildProcurementPayloadJson(inventory, request)
                    )
            );

            notificationService.notifyAdminsAndManagers(
                    "REQUEST",
                    "Procurement Approval Required",
                    actor.getFullName() + " requested procurement of " + inventory.getName() + ".",
                    "APPROVAL_REQUEST",
                    created.id()
            );

            return new ActionSubmissionResponse(
                    false,
                    true,
                    "Procurement request submitted for approval.",
                    created.id()
            );
        }

        inventoryProcurementExecutorService.executeProcurement(inventoryId, request, actor);

        notificationService.notifyAllUsers(
                "INVENTORY",
                "Procurement Completed",
                "Inventory procurement completed for " + inventory.getName(),
                "INVENTORY",
                inventoryId
        );

        return new ActionSubmissionResponse(
                true,
                false,
                "Procurement saved successfully.",
                null
        );
    }

    private String buildProcurementPayloadJson(Inventory inventory, InventoryProcurementRequest request) {
        return """
            {
              "inventoryId": %d,
              "quantityAdded": %d,
              "totalCost": %s,
              "categoryId": %d,
              "incidentId": %s,
              "calamityId": %s,
              "expenseDate": "%s",
              "description": "%s"
            }
            """.formatted(
                inventory.getId(),
                request.quantityAdded(),
                request.totalCost(),
                request.categoryId(),
                request.incidentId() != null ? request.incidentId().toString() : "null",
                request.calamityId() != null ? request.calamityId().toString() : "null",
                request.expenseDate() != null ? request.expenseDate().toString() : "",
                escapeJson(request.description() != null ? request.description() : "")
        );
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}