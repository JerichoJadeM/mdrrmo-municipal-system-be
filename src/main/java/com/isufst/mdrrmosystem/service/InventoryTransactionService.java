package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.InventoryTransaction;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.InventoryTransactionRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.request.InventoryAdjustmentRequest;
import com.isufst.mdrrmosystem.response.InventoryResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class InventoryTransactionService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ApprovalRequestService approvalRequestService;
    private final NotificationService notificationService;

    public InventoryTransactionService(InventoryTransactionRepository inventoryTransactionRepository,
                                       InventoryRepository inventoryRepository,
                                       IncidentRepository incidentRepository,
                                       UserRepository userRepository,
                                       FindAuthenticatedUser findAuthenticatedUser,
                                       ApprovalRequestService approvalRequestService,
                                       NotificationService notificationService) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryRepository = inventoryRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.approvalRequestService = approvalRequestService;
        this.notificationService = notificationService;
    }

    @Transactional
    public InventoryResponse adjustStock(long inventoryId, InventoryAdjustmentRequest request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found: " + inventoryId));

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        boolean isElevated = actor.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        if (!isElevated) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only manager/admin can adjust stock directly.");
        }

        String actionType = request.actionType().trim().toUpperCase();
        int quantity = request.quantity();

        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than 0.");
        }

        switch (actionType) {
            case "RESTOCK", "RETURN" -> {
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
                inventory.setTotalQuantity(Math.max(inventory.getTotalQuantity(), inventory.getAvailableQuantity()));
            }
            case "DEPLOY", "CONSUMED", "DAMAGED", "ADJUSTMENT" -> {
                if (inventory.getAvailableQuantity() < quantity) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough inventory stock.");
                }
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action type: " + actionType);
        }

        inventoryRepository.save(inventory);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setActionType(actionType);
        tx.setQuantity(quantity);
        tx.setTimeStamp(LocalDateTime.now());
        tx.setInventory(inventory);

        Incident incident = null;
        if (request.incidentId() != null) {
            incident = incidentRepository.findById(request.incidentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found: " + request.incidentId()));
            tx.setIncident(incident);
        }

        tx.setPerformedBy(actor);
        inventoryTransactionRepository.save(tx);

        notificationService.notifyAllUsers(
                "INVENTORY",
                "Stock Updated",
                actor.getFullName() + " updated stock for " + inventory.getName() + " (" + actionType + ", qty: " + quantity + ").",
                "INVENTORY",
                inventory.getId()
        );

        return new InventoryResponse(
                inventory.getId(),
                inventory.getName(),
                inventory.getCategory(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getUnit(),
                inventory.getLocation(),
                inventory.getReorderLevel(),
                inventory.getCriticalItem(),
                inventory.getAvailableQuantity() <= 0 ? "OUT"
                        : inventory.getAvailableQuantity() <= (inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0) ? "LOW" : "OK",
                inventory.getEstimatedUnitCost()
        );
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}