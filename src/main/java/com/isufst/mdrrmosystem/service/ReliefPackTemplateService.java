package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.request.ReliefPackTemplateItemRequest;
import com.isufst.mdrrmosystem.request.ReliefPackTemplateRequest;
import com.isufst.mdrrmosystem.response.*;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReliefPackTemplateService {

    private final ReliefPackTemplateRepository templateRepository;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryRepository inventoryRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ApprovalRequestService approvalRequestService;
    private final NotificationService notificationService;
    private final ReliefPackDistributionExecutorService reliefPackDistributionExecutorService;

    public ReliefPackTemplateService(ReliefPackTemplateRepository templateRepository,
                                     ReliefDistributionRepository reliefDistributionRepository,
                                     InventoryTransactionRepository inventoryTransactionRepository,
                                     InventoryRepository inventoryRepository,
                                     IncidentRepository incidentRepository,
                                     CalamityRepository calamityRepository,
                                     EvacuationActivationRepository evacuationActivationRepository,
                                     FindAuthenticatedUser findAuthenticatedUser,
                                     ApprovalRequestService approvalRequestService,
                                     NotificationService notificationService,
                                     ReliefPackDistributionExecutorService reliefPackDistributionExecutorService) {
        this.templateRepository = templateRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryRepository = inventoryRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.approvalRequestService = approvalRequestService;
        this.notificationService = notificationService;
        this.reliefPackDistributionExecutorService = reliefPackDistributionExecutorService;
    }

    @Transactional
    public ReliefPackTemplateResponse createTemplate(ReliefPackTemplateRequest request) {
        validateRequest(request);

        ReliefPackTemplate template = new ReliefPackTemplate();
        template.setName(request.name().trim());
        template.setPackType(request.packType().trim().toUpperCase());
        template.setIntendedUse(request.intendedUse().trim().toUpperCase());
        template.setActive(request.active() == null || request.active());

        List<ReliefPackTemplateItem> items = new ArrayList<>();

        for (ReliefPackTemplateItemRequest itemRequest : request.items()) {
            Inventory inventory = inventoryRepository.findById(itemRequest.inventoryId())
                    .orElseThrow(() -> new RuntimeException("Inventory not found: " + itemRequest.inventoryId()));

            if (itemRequest.quantityRequired() == null || itemRequest.quantityRequired() <= 0) {
                throw new RuntimeException("Quantity required must be greater than 0");
            }

            ReliefPackTemplateItem item = new ReliefPackTemplateItem();
            item.setTemplate(template);
            item.setInventory(inventory);
            item.setQuantityRequired(itemRequest.quantityRequired());

            items.add(item);
        }

        template.setItems(items);

        ReliefPackTemplate saved = templateRepository.save(template);
        return mapTemplate(saved);
    }

    @Transactional(readOnly = true)
    public List<ReliefPackTemplateResponse> getTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapTemplate)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReliefPackTemplateResponse> getActiveTemplates() {
        return templateRepository.findByActiveTrue().stream()
                .map(this::mapTemplate)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReliefPackReadinessResponse getReadiness(Long templateId) {
        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        if (template.getItems() == null || template.getItems().isEmpty()) {
            return new ReliefPackReadinessResponse(
                    template.getId(),
                    template.getName(),
                    template.getPackType(),
                    template.getIntendedUse(),
                    0,
                    null,
                    0,
                    true,
                    List.of()
            );
        }

        int maxProduciblePacks = Integer.MAX_VALUE;
        String limitingItemName = null;
        double estimatedPackCost = 0;
        boolean hasCompletedCostData = true;

        List<ReliefPackReadinessItemResponse> readinessItems = new ArrayList<>();

        for (ReliefPackTemplateItem item : template.getItems()) {
            Inventory inventory = item.getInventory();
            int quantityRequired = item.getQuantityRequired();
            int availableQuantity = inventory.getAvailableQuantity();
            int producible = quantityRequired > 0 ? availableQuantity / quantityRequired : 0;

            if (producible < maxProduciblePacks) {
                maxProduciblePacks = producible;
                limitingItemName = inventory.getName();
            }

            Double unitCost = inventory.getEstimatedUnitCost() != null ? inventory.getEstimatedUnitCost() : 0;

            if(unitCost == null || unitCost <= 0){
                hasCompletedCostData = false;
            } else {
                estimatedPackCost += unitCost * quantityRequired;
            }

            readinessItems.add(new ReliefPackReadinessItemResponse(
                    inventory.getId(),
                    inventory.getName(),
                    quantityRequired,
                    availableQuantity,
                    producible,
                    false,
                    inventory.getEstimatedUnitCost()
            ));
        }

        final int limitingCount = maxProduciblePacks;

        List<ReliefPackReadinessItemResponse> finalItems = readinessItems.stream()
                .map(item -> new ReliefPackReadinessItemResponse(
                        item.inventoryId(),
                        item.inventoryName(),
                        item.quantityRequiredPerPack(),
                        item.availableQuantity(),
                        item.produciblePacksFromThisItem(),
                        item.produciblePacksFromThisItem() == limitingCount,
                        item.estimatedUnitCost()
                ))
                .sorted(Comparator.comparing(ReliefPackReadinessItemResponse::limitingItem).reversed())
                .toList();

        return new ReliefPackReadinessResponse(
                template.getId(),
                template.getName(),
                template.getPackType(),
                template.getIntendedUse(),
                maxProduciblePacks,
                limitingItemName,
                estimatedPackCost,
                hasCompletedCostData,
                finalItems
        );
    }

    private void validateRequest(ReliefPackTemplateRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new RuntimeException("Template name is required");
        }
        if (request.packType() == null || request.packType().isBlank()) {
            throw new RuntimeException("Pack type is required");
        }
        if (request.intendedUse() == null || request.intendedUse().isBlank()) {
            throw new RuntimeException("Intended use is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new RuntimeException("At least one pack item is required");
        }
    }

    @Transactional
    public ActionSubmissionResponse distributeTemplateForIncident(Long templateId, Long incidentId, Integer packCount, Long evacuationActivationId) {
        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        boolean isElevated = actor.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        if (!isElevated) {
            ApprovalRequestResponse created = approvalRequestService.createRequest(
                    new ApprovalRequestCreateRequest(
                            "RELIEF_PACK_DISTRIBUTION_REQUEST",
                            "Relief pack distribution request for " + template.getName(),
                            "Request to distribute " + packCount + " pack(s) for incident " + incident.getType(),
                            "INCIDENT",
                            incident.getId(),
                            """
                            {
                              "templateId": %d,
                              "incidentId": %d,
                              "packCount": %d,
                              "evacuationActivationId": %s
                            }
                            """.formatted(
                                    templateId,
                                    incidentId,
                                    packCount,
                                    evacuationActivationId != null ? evacuationActivationId.toString() : "null"
                            )
                    )
            );

            notificationService.notifyAdminsAndManagers(
                    "REQUEST",
                    "Relief Pack Approval Required",
                    actor.getFullName() + " requested distribution of relief pack " + template.getName() + ".",
                    "APPROVAL_REQUEST",
                    created.id()
            );

            return new ActionSubmissionResponse(false, true, "Relief pack distribution request submitted for approval.", created.id());
        }

        reliefPackDistributionExecutorService.executeIncidentPackDistribution(templateId, incidentId, packCount, evacuationActivationId, actor);

        notificationService.notifyAllUsers(
                "INVENTORY",
                "Relief Pack Distribution Completed",
                "Relief pack distribution completed for template " + template.getName(),
                "INCIDENT",
                incidentId
        );

        return new ActionSubmissionResponse(true, false, "Relief pack distributed successfully.", null);
    }

    @Transactional
    public ActionSubmissionResponse distributeTemplateForCalamity(Long templateId, Long calamityId, Integer packCount, Long evacuationActivationId) {
        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found: " + calamityId));

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        boolean isElevated = actor.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        if (!isElevated) {
            ApprovalRequestResponse created = approvalRequestService.createRequest(
                    new ApprovalRequestCreateRequest(
                            "RELIEF_PACK_DISTRIBUTION_REQUEST",
                            "Relief pack distribution request for " + template.getName(),
                            "Request to distribute " + packCount + " pack(s) for calamity " + calamity.getType(),
                            "CALAMITY",
                            calamity.getId(),
                            """
                            {
                              "templateId": %d,
                              "calamityId": %d,
                              "packCount": %d,
                              "evacuationActivationId": %s
                            }
                            """.formatted(
                                    templateId,
                                    calamityId,
                                    packCount,
                                    evacuationActivationId != null ? evacuationActivationId.toString() : "null"
                            )
                    )
            );

            notificationService.notifyAdminsAndManagers(
                    "REQUEST",
                    "Relief Pack Approval Required",
                    actor.getFullName() + " requested distribution of relief pack " + template.getName() + ".",
                    "APPROVAL_REQUEST",
                    created.id()
            );

            return new ActionSubmissionResponse(false, true, "Relief pack distribution request submitted for approval.", created.id());
        }

        reliefPackDistributionExecutorService.executeCalamityPackDistribution(templateId, calamityId, packCount, evacuationActivationId, actor);

        notificationService.notifyAllUsers(
                "INVENTORY",
                "Relief Pack Distribution Completed",
                "Relief pack distribution completed for template " + template.getName(),
                "CALAMITY",
                calamityId
        );

        return new ActionSubmissionResponse(true, false, "Relief pack distributed successfully.", null);
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void validatePackStock(ReliefPackTemplate template, int packCount) {
        if (template.getItems() == null || template.getItems().isEmpty()) {
            throw new RuntimeException("Relief pack template has no items");
        }

        for (ReliefPackTemplateItem item : template.getItems()) {
            Inventory inventory = item.getInventory();
            int totalRequired = item.getQuantityRequired() * packCount;

            if (inventory.getAvailableQuantity() < totalRequired) {
                throw new RuntimeException(
                        "Insufficient stock for " + inventory.getName() +
                                ". Required: " + totalRequired +
                                ", Available: " + inventory.getAvailableQuantity()
                );
            }
        }
    }

    @Transactional
    public ReliefPackTemplateResponse updateTemplate(Long templateId, ReliefPackTemplateRequest request) {
        validateRequest(request);

        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        template.setName(request.name().trim());
        template.setPackType(request.packType().trim().toUpperCase());
        template.setIntendedUse(request.intendedUse().trim().toUpperCase());
        template.setActive(request.active() == null || request.active());

        template.getItems().clear();

        for (ReliefPackTemplateItemRequest itemRequest : request.items()) {
            Inventory inventory = inventoryRepository.findById(itemRequest.inventoryId())
                    .orElseThrow(() -> new RuntimeException("Inventory not found: " + itemRequest.inventoryId()));

            ReliefPackTemplateItem item = new ReliefPackTemplateItem();
            item.setTemplate(template);
            item.setInventory(inventory);
            item.setQuantityRequired(itemRequest.quantityRequired());

            template.getItems().add(item);
        }

        ReliefPackTemplate saved = templateRepository.save(template);
        return mapTemplate(saved);
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        templateRepository.delete(template);
    }

    private ReliefPackTemplateResponse mapTemplate(ReliefPackTemplate template) {
        return new ReliefPackTemplateResponse(
                template.getId(),
                template.getName(),
                template.getPackType(),
                template.getIntendedUse(),
                template.isActive(),
                template.getItems() == null
                        ? List.of()
                        : template.getItems().stream()
                        .map(item -> new ReliefPackTemplateItemResponse(
                                item.getId(),
                                item.getInventory().getId(),
                                item.getInventory().getName(),
                                item.getInventory().getUnit(),
                                item.getQuantityRequired(),
                                item.getInventory().getAvailableQuantity(),
                                item.getInventory().getEstimatedUnitCost()
                        ))
                        .toList()
        );
    }
}
