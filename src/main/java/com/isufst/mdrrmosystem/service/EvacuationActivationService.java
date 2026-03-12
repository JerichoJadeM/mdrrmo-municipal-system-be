package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.EvacuationCenter;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.EvacuationCenterRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.request.EvacuationActivationRequest;
import com.isufst.mdrrmosystem.request.UpdateEvacueesRequest;
import com.isufst.mdrrmosystem.response.EvacuationActivationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EvacuationActivationService {

    private final EvacuationCenterRepository evacuationCenterRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final IncidentRepository incidentRepository;

    public EvacuationActivationService(EvacuationCenterRepository evacuationCenterRepository,
                                       EvacuationActivationRepository evacuationActivationRepository,
                                       IncidentRepository incidentRepository){
        this.evacuationCenterRepository = evacuationCenterRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public EvacuationActivationResponse activateCenter(Long id, EvacuationActivationRequest request){
        Incident incident = incidentRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Incident not found"));

        EvacuationCenter center = evacuationCenterRepository.findById(request.centerId())
                .orElseThrow(() -> new RuntimeException("Center not found"));

        EvacuationActivation activation = new EvacuationActivation();
        activation.setIncident(incident);
        activation.setCenter(center);
        activation.setCurrentEvacuees(request.initialEvacuees());
        activation.setStatus("OPEN");
        activation.setOpenedAt(LocalDateTime.now());

        evacuationActivationRepository.save(activation);

        return mapToResponse(activation);
    }

    @Transactional
    public EvacuationActivationResponse updateEvacuees(Long activationId, UpdateEvacueesRequest request){
        EvacuationActivation activ = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new RuntimeException("Activation id not found"));

        activ.setCurrentEvacuees(request.evacueeCount());

        return mapToResponse(evacuationActivationRepository.save(activ));
    }

    @Transactional
    public EvacuationActivationResponse closeCenter(Long activationId){
        EvacuationActivation activation = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new RuntimeException("Activation id not found"));

        activation.setStatus("CLOSED");
        activation.setClosedAt(LocalDateTime.now());

        return mapToResponse(evacuationActivationRepository.save(activation));
    }

    public List<EvacuationActivationResponse> getByIncident(Long incidentId){
        return evacuationActivationRepository.findByIncident_Id(incidentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private EvacuationActivationResponse mapToResponse(EvacuationActivation activation) {

        return new EvacuationActivationResponse(
                activation.getId(),
                activation.getCenter().getName(),
                activation.getCenter().getBarangay(),
                activation.getCenter().getCapacity(),
                activation.getCurrentEvacuees(),
                activation.getStatus(),
                activation.getOpenedAt(),
                activation.getClosedAt()
        );
    }
}
