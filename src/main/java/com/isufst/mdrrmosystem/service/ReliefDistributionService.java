package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.request.ReliefDistributionRequest;
import com.isufst.mdrrmosystem.response.ActionSubmissionResponse;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;
import com.isufst.mdrrmosystem.response.ReliefDistributionResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import jakarta.persistence.Entity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReliefDistributionService {

    private final ReliefDistributionRepository repository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final EvacuationActivationRepository activationRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ApprovalRequestService approvalRequestService;
    private final NotificationService notificationService;
    private final ReliefDistributionExecutorService reliefDistributionExecutorService;

    public ReliefDistributionService(
            ReliefDistributionRepository repository,
            IncidentRepository incidentRepository,
            CalamityRepository calamityRepository,
            EvacuationActivationRepository activationRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            FindAuthenticatedUser findAuthenticatedUser,
            ApprovalRequestService approvalRequestService,
            NotificationService notificationService,
            ReliefDistributionExecutorService reliefDistributionExecutorService) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.activationRepository = activationRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.approvalRequestService = approvalRequestService;
        this.notificationService = notificationService;
        this.reliefDistributionExecutorService = reliefDistributionExecutorService;
    }

    @Transactional
    public ActionSubmissionResponse distribute(Long incidentId, ReliefDistributionRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        User actor = findAuthenticatedUser.getAuthenticatedUser();

        Inventory inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        boolean isElevated = actor.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        if (!isElevated) {
            ApprovalRequestResponse created = approvalRequestService.createRequest(
                    new ApprovalRequestCreateRequest(
                            "RELIEF_DISTRIBUTION_REQUEST",
                            "Relief distribution request for " + inventory.getName(),
                            "Request to distribute " + request.quantity() + " " + inventory.getUnit() + " for incident " + incident.getType(),
                            "INCIDENT",
                            incident.getId(),
                            """
                            {
                              "incidentId": %d,
                              "inventoryId": %d,
                              "quantity": %d,
                              "evacuationActivationId": %s
                            }
                            """.formatted(
                                    incidentId,
                                    request.inventoryId(),
                                    request.quantity(),
                                    request.evacuationActivationId() != null ? request.evacuationActivationId().toString() : "null"
                            )
                    )
            );

            notificationService.notifyAdminsAndManagers(
                    "REQUEST",
                    "Relief Distribution Approval Required",
                    actor.getFullName() + " requested distribution of " + inventory.getName() + ".",
                    "APPROVAL_REQUEST",
                    created.id()
            );

            return new ActionSubmissionResponse(false, true, "Relief distribution request submitted for approval.", created.id());
        }

        reliefDistributionExecutorService.executeIncidentDistribution(incidentId, request, actor);

        notificationService.notifyAllUsers(
                "INVENTORY",
                "Relief Distribution Completed",
                "Relief distribution completed for " + inventory.getName(),
                "INCIDENT",
                incidentId
        );

        return new ActionSubmissionResponse(true, false, "Relief distributed successfully.", null);
    }

    public List<ReliefDistributionResponse> getByIncident(Long incidentId) {
        return repository.findByIncident_Id(incidentId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public ActionSubmissionResponse distributeForCalamity(Long calamityId, ReliefDistributionRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found"));

        User actor = findAuthenticatedUser.getAuthenticatedUser();

        Inventory inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        boolean isElevated = actor.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        if (!isElevated) {
            ApprovalRequestResponse created = approvalRequestService.createRequest(
                    new ApprovalRequestCreateRequest(
                            "RELIEF_DISTRIBUTION_REQUEST",
                            "Relief distribution request for " + inventory.getName(),
                            "Request to distribute " + request.quantity() + " " + inventory.getUnit() + " for calamity " + calamity.getType(),
                            "CALAMITY",
                            calamity.getId(),
                            """
                            {
                              "calamityId": %d,
                              "inventoryId": %d,
                              "quantity": %d,
                              "evacuationActivationId": %s
                            }
                            """.formatted(
                                    calamityId,
                                    request.inventoryId(),
                                    request.quantity(),
                                    request.evacuationActivationId() != null ? request.evacuationActivationId().toString() : "null"
                            )
                    )
            );

            notificationService.notifyAdminsAndManagers(
                    "REQUEST",
                    "Relief Distribution Approval Required",
                    actor.getFullName() + " requested distribution of " + inventory.getName() + ".",
                    "APPROVAL_REQUEST",
                    created.id()
            );

            return new ActionSubmissionResponse(false, true, "Relief distribution request submitted for approval.", created.id());
        }

        reliefDistributionExecutorService.executeCalamityDistribution(calamityId, request, actor);

        notificationService.notifyAllUsers(
                "INVENTORY",
                "Relief Distribution Completed",
                "Relief distribution completed for " + inventory.getName(),
                "CALAMITY",
                calamityId
        );

        return new ActionSubmissionResponse(true, false, "Relief distributed successfully.", null);
    }

    public List<ReliefDistributionResponse> getByCalamity(Long calamityId) {
        return repository.findByCalamity_Id(calamityId)
                .stream()
                .map(this::map)
                .toList();
    }

    private ReliefDistributionResponse map(ReliefDistribution r) {
        String operationLabel = r.getIncident() != null
                ? r.getIncident().getType()
                : r.getCalamity() != null
                ? r.getCalamity().getType()
                : "N/A";

        return new ReliefDistributionResponse(
                r.getId(),
                r.getInventory().getId(),
                r.getInventory().getName(),
                r.getQuantity(),
                r.getDistributedAt(),
                operationLabel,
                r.getEvacuationActivation() != null
                        ? r.getEvacuationActivation().getCenter().getName()
                        : "N/A",
                r.getDistributedBy().getUsername()
        );
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
