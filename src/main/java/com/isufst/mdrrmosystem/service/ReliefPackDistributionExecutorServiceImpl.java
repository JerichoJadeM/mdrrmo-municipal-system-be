package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReliefPackDistributionExecutorServiceImpl implements ReliefPackDistributionExecutorService {

    private final ReliefPackTemplateRepository templateRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final InventoryRepository inventoryRepository;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public ReliefPackDistributionExecutorServiceImpl(ReliefPackTemplateRepository templateRepository,
                                                     IncidentRepository incidentRepository,
                                                     CalamityRepository calamityRepository,
                                                     EvacuationActivationRepository evacuationActivationRepository,
                                                     InventoryRepository inventoryRepository,
                                                     ReliefDistributionRepository reliefDistributionRepository,
                                                     InventoryTransactionRepository inventoryTransactionRepository) {
        this.templateRepository = templateRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.inventoryRepository = inventoryRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    @Transactional
    public void executeIncidentPackDistribution(Long templateId, Long incidentId, Integer packCount, Long evacuationActivationId, User actor) {
        if (packCount == null || packCount <= 0) {
            throw new RuntimeException("Pack count must be greater than 0");
        }

        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));

        EvacuationActivation activation = null;
        if (evacuationActivationId != null) {
            activation = evacuationActivationRepository.findById(evacuationActivationId).orElse(null);
        }

        validatePackStock(template, packCount);

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
            relief.setDistributedBy(actor);
            reliefDistributionRepository.save(relief);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setActionType("CONSUMED");
            transaction.setQuantity(totalRequired);
            transaction.setTimeStamp(LocalDateTime.now());
            transaction.setInventory(inventory);
            transaction.setIncident(incident);
            transaction.setPerformedBy(actor);
            transaction.setDistribution(relief);
            inventoryTransactionRepository.save(transaction);
        }
    }

    @Override
    @Transactional
    public void executeCalamityPackDistribution(Long templateId, Long calamityId, Integer packCount, Long evacuationActivationId, User actor) {
        if (packCount == null || packCount <= 0) {
            throw new RuntimeException("Pack count must be greater than 0");
        }

        ReliefPackTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Relief pack template not found: " + templateId));

        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found: " + calamityId));

        EvacuationActivation activation = null;
        if (evacuationActivationId != null) {
            activation = evacuationActivationRepository.findById(evacuationActivationId).orElse(null);
        }

        validatePackStock(template, packCount);

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
            relief.setDistributedBy(actor);
            reliefDistributionRepository.save(relief);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setActionType("CONSUMED");
            transaction.setQuantity(totalRequired);
            transaction.setTimeStamp(LocalDateTime.now());
            transaction.setInventory(inventory);
            transaction.setIncident(null);
            transaction.setPerformedBy(actor);
            transaction.setDistribution(relief);
            inventoryTransactionRepository.save(transaction);
        }
    }

    private void validatePackStock(ReliefPackTemplate template, Integer packCount) {
        for (ReliefPackTemplateItem item : template.getItems()) {
            int required = item.getQuantityRequired() * packCount;
            Inventory inventory = item.getInventory();

            if (inventory.getAvailableQuantity() < required) {
                throw new RuntimeException("Insufficient stock for " + inventory.getName());
            }
        }
    }
}