package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ReliefDistributionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReliefDistributionExecutorServiceImpl implements ReliefDistributionExecutorService {

    private final ReliefDistributionRepository repository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final EvacuationActivationRepository activationRepository;

    public ReliefDistributionExecutorServiceImpl(ReliefDistributionRepository repository,
                                                 IncidentRepository incidentRepository,
                                                 CalamityRepository calamityRepository,
                                                 InventoryRepository inventoryRepository,
                                                 InventoryTransactionRepository inventoryTransactionRepository,
                                                 EvacuationActivationRepository activationRepository) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.activationRepository = activationRepository;
    }

    @Override
    @Transactional
    public void executeIncidentDistribution(Long incidentId, ReliefDistributionRequest request, User actor) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        Inventory inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new RuntimeException("Insufficient inventory stock");
        }

        EvacuationActivation activation = null;
        if (request.evacuationActivationId() != null) {
            activation = activationRepository.findById(request.evacuationActivationId()).orElse(null);
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.quantity());
        inventoryRepository.save(inventory);

        ReliefDistribution relief = new ReliefDistribution();
        relief.setInventory(inventory);
        relief.setQuantity(request.quantity());
        relief.setDistributedAt(LocalDateTime.now());
        relief.setIncident(incident);
        relief.setCalamity(null);
        relief.setEvacuationActivation(activation);
        relief.setDistributedBy(actor);
        repository.save(relief);

        InventoryTransaction invTrans = new InventoryTransaction();
        invTrans.setActionType("CONSUMED");
        invTrans.setQuantity(request.quantity());
        invTrans.setTimeStamp(LocalDateTime.now());
        invTrans.setInventory(inventory);
        invTrans.setIncident(incident);
        invTrans.setPerformedBy(actor);
        invTrans.setDistribution(relief);
        inventoryTransactionRepository.save(invTrans);
    }

    @Override
    @Transactional
    public void executeCalamityDistribution(Long calamityId, ReliefDistributionRequest request, User actor) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found"));

        Inventory inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new RuntimeException("Insufficient inventory stock");
        }

        EvacuationActivation activation = null;
        if (request.evacuationActivationId() != null) {
            activation = activationRepository.findById(request.evacuationActivationId()).orElse(null);
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.quantity());
        inventoryRepository.save(inventory);

        ReliefDistribution relief = new ReliefDistribution();
        relief.setInventory(inventory);
        relief.setQuantity(request.quantity());
        relief.setDistributedAt(LocalDateTime.now());
        relief.setIncident(null);
        relief.setCalamity(calamity);
        relief.setEvacuationActivation(activation);
        relief.setDistributedBy(actor);
        repository.save(relief);

        InventoryTransaction invTrans = new InventoryTransaction();
        invTrans.setActionType("CONSUMED");
        invTrans.setQuantity(request.quantity());
        invTrans.setTimeStamp(LocalDateTime.now());
        invTrans.setInventory(inventory);
        invTrans.setIncident(null);
        invTrans.setPerformedBy(actor);
        invTrans.setDistribution(relief);
        inventoryTransactionRepository.save(invTrans);
    }
}
