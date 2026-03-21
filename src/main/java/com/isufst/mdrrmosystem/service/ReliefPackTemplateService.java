package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ReliefPackTemplateItemRequest;
import com.isufst.mdrrmosystem.request.ReliefPackTemplateRequest;
import com.isufst.mdrrmosystem.response.ReliefPackReadinessItemResponse;
import com.isufst.mdrrmosystem.response.ReliefPackReadinessResponse;
import com.isufst.mdrrmosystem.response.ReliefPackTemplateItemResponse;
import com.isufst.mdrrmosystem.response.ReliefPackTemplateResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ReliefPackTemplateService(ReliefPackTemplateRepository templateRepository,
                                     ReliefDistributionRepository reliefDistributionRepository,
                                     InventoryTransactionRepository inventoryTransactionRepository,
                                     InventoryRepository inventoryRepository,
                                     IncidentRepository incidentRepository,
                                     CalamityRepository calamityRepository,
                                     EvacuationActivationRepository evacuationActivationRepository,
                                     FindAuthenticatedUser findAuthenticatedUser) {
        this.templateRepository = templateRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryRepository = inventoryRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
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
    public void distributeTemplateForIncident(Long templateId, Long incidentId, Integer packCount, Long evacuationActivationId) {
        if (packCount == null || packCount <= 0) {
            throw new RuntimeException("Pack count must be greater than 0");
        }

        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));

        EvacuationActivation activation = null;
        if (evacuationActivationId != null) {
            activation = evacuationActivationRepository.findById(evacuationActivationId)
                    .orElseThrow(() -> new RuntimeException("Evacuation activation not found: " + evacuationActivationId));
        }

        validatePackStock(template, packCount);

        User user = findAuthenticatedUser.getAuthenticatedUser();

        for (ReliefPackTemplateItem item : template.getItems()) {
            Inventory inventory = item.getInventory();
            int totalRequired = item.getQuantityRequired() * packCount;

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - totalRequired);
            inventoryRepository.save(inventory);

            ReliefDistribution relief = new ReliefDistribution();
            relief.setInventory(inventory);
            relief.setQuantity(totalRequired);
            relief.setDistributedAt(LocalDateTime.now());
            relief.setIncident(incident);
            relief.setCalamity(null);
            relief.setEvacuationActivation(activation);
            relief.setDistributedBy(user);
            reliefDistributionRepository.save(relief);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setActionType("CONSUMED");
            transaction.setQuantity(totalRequired);
            transaction.setTimeStamp(LocalDateTime.now());
            transaction.setInventory(inventory);
            transaction.setIncident(incident);
            transaction.setPerformedBy(user);
            transaction.setDistribution(relief);
            inventoryTransactionRepository.save(transaction);
        }
    }

    @Transactional
    public void distributeTemplateForCalamity(Long templateId, Long calamityId, Integer packCount, Long evacuationActivationId) {
        if (packCount == null || packCount <= 0) {
            throw new RuntimeException("Pack count must be greater than 0");
        }

        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found: " + calamityId));

        EvacuationActivation activation = null;
        if (evacuationActivationId != null) {
            activation = evacuationActivationRepository.findById(evacuationActivationId)
                    .orElseThrow(() -> new RuntimeException("Evacuation activation not found: " + evacuationActivationId));
        }

        validatePackStock(template, packCount);

        User user = findAuthenticatedUser.getAuthenticatedUser();

        for (ReliefPackTemplateItem item : template.getItems()) {
            Inventory inventory = item.getInventory();
            int totalRequired = item.getQuantityRequired() * packCount;

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - totalRequired);
            inventoryRepository.save(inventory);

            ReliefDistribution relief = new ReliefDistribution();
            relief.setInventory(inventory);
            relief.setQuantity(totalRequired);
            relief.setDistributedAt(LocalDateTime.now());
            relief.setIncident(null);
            relief.setCalamity(calamity);
            relief.setEvacuationActivation(activation);
            relief.setDistributedBy(user);
            reliefDistributionRepository.save(relief);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setActionType("CONSUMED");
            transaction.setQuantity(totalRequired);
            transaction.setTimeStamp(LocalDateTime.now());
            transaction.setInventory(inventory);
            transaction.setIncident(null);
            transaction.setPerformedBy(user);
            transaction.setDistribution(relief);
            inventoryTransactionRepository.save(transaction);
        }
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
