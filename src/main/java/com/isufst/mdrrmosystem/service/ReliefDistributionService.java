package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ReliefDistribution;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.ReliefDistributionRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
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

    public ReliefDistributionService(
            ReliefDistributionRepository repository,
            IncidentRepository incidentRepository,
            EvacuationActivationRepository activationRepository,
            UserRepository userRepository) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
        this.activationRepository = activationRepository;
        this.userRepository = userRepository;
    }

    public ReliefDistributionResponse distribute(
            Long incidentId,
            ReliefDistributionRequest request) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        User user = userRepository.findById(request.distributedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        EvacuationActivation activation = null;

        if (request.evacuationActivationId() != null) {
            activation = activationRepository.findById(request.evacuationActivationId())
                    .orElseThrow(() -> new RuntimeException("Activation not found"));
        }

        ReliefDistribution relief = new ReliefDistribution();
        relief.setItemType(request.itemType());
        relief.setQuantity(request.quantity());
        relief.setDistributedAt(LocalDateTime.now());
        relief.setIncident(incident);
        relief.setEvacuationActivation(activation);
        relief.setDistributedBy(user);

        repository.save(relief);

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
                r.getItemType(),
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
