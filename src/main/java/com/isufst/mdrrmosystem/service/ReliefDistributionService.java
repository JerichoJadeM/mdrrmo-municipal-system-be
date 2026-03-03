package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.ReliefDistributionRequest;
import com.isufst.mdrrmosystem.response.ReliefDistributionResponse;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReliefDistributionService {

    private final ReliefDistributionRepository repository;
    private final IncidentRepository incidentRepository;
    private final EvacuationActivationRepository activationRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public ReliefDistributionService(
            ReliefDistributionRepository repository,
            IncidentRepository incidentRepository,
            EvacuationActivationRepository activationRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
        this.activationRepository = activationRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    public ReliefDistributionResponse distribute(
            Long incidentId,
            ReliefDistributionRequest request) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        User user = userRepository.findById(request.distributedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Inventory inventory = inventoryRepository.findById(request.inventoryId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        EvacuationActivation activation = null;

        if (request.evacuationActivationId() != null) {
            activation = activationRepository.findById(request.evacuationActivationId())
                    .orElseThrow(() -> new RuntimeException("Activation not found"));
        }

        // stock validation
        if(request.quantity() <= 0){
            throw new RuntimeException("Quantity must be greater than 0");
        }

        if(inventory.getAvailableQuantity() < request.quantity()){
            throw new RuntimeException("Insufficient inventory stock");
        }

        // deduct stock
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.quantity());
        inventoryRepository.save(inventory);

        ReliefDistribution relief = new ReliefDistribution();
        relief.setInventory(inventory);
        relief.setQuantity(request.quantity());
        relief.setDistributedAt(LocalDateTime.now());
        relief.setIncident(incident);
        relief.setEvacuationActivation(activation);
        relief.setDistributedBy(user);

        repository.save(relief);

        // log inventory movement
        InventoryTransaction invTrans = new  InventoryTransaction();

        invTrans.setActionType("CONSUMED");
        invTrans.setQuantity(request.quantity());
        invTrans.setTimeStamp(LocalDateTime.now());
        invTrans.setInventory(inventory);
        invTrans.setIncident(incident);
        invTrans.setPerformedBy(user);
        invTrans.setDistribution(relief);

        inventoryTransactionRepository.save(invTrans);

        return map(relief);
    }

    public List<ReliefDistributionResponse> getByIncident(Long incidentId) {
        return repository.findByIncident_Id(incidentId)
                .stream()
                .map(this::map)
                .toList();
    }

    private ReliefDistributionResponse map(ReliefDistribution r) {
        return new ReliefDistributionResponse(
                r.getId(),
                r.getInventory().getId(),
                r.getInventory().getName(),
                r.getQuantity(),
                r.getDistributedAt(),
                r.getIncident().getType(),
                r.getEvacuationActivation() != null
                        ? r.getEvacuationActivation().getCenter().getName()
                        : "N/A",
                r.getDistributedBy().getUsername()
        );
    }
}
