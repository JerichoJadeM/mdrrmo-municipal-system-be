package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.EvacuationCenter;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.EvacuationCenterRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.request.EvacuationActivationRequest;
import com.isufst.mdrrmosystem.request.UpdateEvacueesRequest;
import com.isufst.mdrrmosystem.response.EvacuationActivationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EvacuationActivationService {

    private final EvacuationActivationRepository evacuationActivationRepository;
    private final EvacuationCenterRepository evacuationCenterRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final OperationHistoryService operationHistoryService;

    public EvacuationActivationService(EvacuationActivationRepository evacuationActivationRepository,
                                       EvacuationCenterRepository evacuationCenterRepository,
                                       IncidentRepository incidentRepository,
                                       CalamityRepository calamityRepository,
                                       OperationHistoryService operationHistoryService) {
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.evacuationCenterRepository = evacuationCenterRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.operationHistoryService = operationHistoryService;
    }

    @Transactional
    public EvacuationActivationResponse activateCenter(Long incidentId, EvacuationActivationRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found: " + incidentId));

        EvacuationCenter center = evacuationCenterRepository.findById(request.centerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation center not found: " + request.centerId()));

        EvacuationActivation activation = new EvacuationActivation();
        activation.setIncident(incident);
        activation.setCalamity(null);
        activation.setCenter(center);
        activation.setCurrentEvacuees(request.currentEvacuees());
        activation.setStatus("OPEN");
        activation.setOpenedAt(LocalDateTime.now());
        activation.setClosedAt(null);

        EvacuationActivation saved = evacuationActivationRepository.save(activation);

        operationHistoryService.log(
                "INCIDENT",
                incident.getId(),
                "EVAC_CENTER_OPENED",
                null,
                null,
                "Evacuation center activated: " + center.getName(),
                "{\"centerId\":" + center.getId() + ",\"currentEvacuees\":" + request.currentEvacuees() + "}",
                null
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EvacuationActivationResponse> getByIncident(Long incidentId) {
        return evacuationActivationRepository.findByIncident_Id(incidentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public EvacuationActivationResponse updateEvacuees(Long incidentId, Long activationId, @Valid UpdateEvacueesRequest request) {
        EvacuationActivation activation = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation activation not found: " + activationId));

        if (activation.getIncident() == null || !(activation.getIncident().getId() == incidentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation does not belong to incident: " + incidentId);
        }

        activation.setCurrentEvacuees(request.currentEvacuees());
        EvacuationActivation saved = evacuationActivationRepository.save(activation);

        operationHistoryService.log(
                "INCIDENT",
                incidentId,
                "EVACUEES_UPDATED",
                null,
                null,
                "Evacuees updated for center: " + activation.getCenter().getName(),
                "{\"activationId\":" + activationId + ",\"currentEvacuees\":" + request.currentEvacuees() + "}",
                null
        );

        return mapToResponse(saved);
    }


    @Transactional
    public EvacuationActivationResponse closeCenter(Long activationId){
        EvacuationActivation activation = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new RuntimeException("Activation id not found"));

        activation.setStatus("CLOSED");
        activation.setClosedAt(LocalDateTime.now());

        return mapToResponse(evacuationActivationRepository.save(activation));
    }

    @Transactional
    public EvacuationActivationResponse closeActivation(Long incidentId, Long activationId) {
        EvacuationActivation activation = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation activation not found: " + activationId));

        if (activation.getIncident() == null || (!(activation.getIncident().getId() == incidentId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation does not belong to incident: " + incidentId);
        }

        activation.setStatus("CLOSED");
        activation.setClosedAt(LocalDateTime.now());
        EvacuationActivation saved = evacuationActivationRepository.save(activation);

        operationHistoryService.log(
                "INCIDENT",
                incidentId,
                "EVAC_CENTER_CLOSED",
                null,
                null,
                "Evacuation center closed: " + activation.getCenter().getName(),
                "{\"activationId\":" + activationId + "}",
                null
        );

        return mapToResponse(saved);
    }

    @Transactional
    public EvacuationActivationResponse activateCenterForCalamity(Long calamityId, EvacuationActivationRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calamity not found: " + calamityId));

        EvacuationCenter center = evacuationCenterRepository.findById(request.centerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation center not found: " + request.centerId()));

        EvacuationActivation activation = new EvacuationActivation();
        activation.setIncident(null);
        activation.setCalamity(calamity);
        activation.setCenter(center);
        activation.setCurrentEvacuees(request.currentEvacuees());
        activation.setStatus("OPEN");
        activation.setOpenedAt(LocalDateTime.now());
        activation.setClosedAt(null);

        EvacuationActivation saved = evacuationActivationRepository.save(activation);

        operationHistoryService.log(
                "CALAMITY",
                calamity.getId(),
                "EVAC_CENTER_OPENED",
                null,
                null,
                "Evacuation center activated: " + center.getName(),
                "{\"centerId\":" + center.getId() + ",\"currentEvacuees\":" + request.currentEvacuees() + "}",
                null
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EvacuationActivationResponse> getByCalamity(Long calamityId) {
        return evacuationActivationRepository.findByCalamity_Id(calamityId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public EvacuationActivationResponse updateEvacueesForCalamity(Long calamityId, Long activationId, UpdateEvacueesRequest request) {
        EvacuationActivation activation = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation activation not found: " + activationId));

        if (activation.getCalamity() == null || !(activation.getCalamity().getId() == calamityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation does not belong to calamity: " + calamityId);
        }

        activation.setCurrentEvacuees(request.currentEvacuees());
        EvacuationActivation saved = evacuationActivationRepository.save(activation);

        operationHistoryService.log(
                "CALAMITY",
                calamityId,
                "EVACUEES_UPDATED",
                null,
                null,
                "Evacuees updated for center: " + activation.getCenter().getName(),
                "{\"activationId\":" + activationId + ",\"currentEvacuees\":" + request.currentEvacuees() + "}",
                null
        );

        return mapToResponse(saved);
    }

    @Transactional
    public EvacuationActivationResponse closeActivationForCalamity(Long calamityId, Long activationId) {
        EvacuationActivation activation = evacuationActivationRepository.findById(activationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation activation not found: " + activationId));

        if (activation.getCalamity() == null || (!(activation.getCalamity().getId() == calamityId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation does not belong to calamity: " + calamityId);
        }

        activation.setStatus("CLOSED");
        activation.setClosedAt(LocalDateTime.now());
        EvacuationActivation saved = evacuationActivationRepository.save(activation);

        operationHistoryService.log(
                "CALAMITY",
                calamityId,
                "EVAC_CENTER_CLOSED",
                null,
                null,
                "Evacuation center closed: " + activation.getCenter().getName(),
                "{\"activationId\":" + activationId + "}",
                null
        );

        return mapToResponse(saved);
    }

    private EvacuationActivationResponse mapToResponse(EvacuationActivation activation) {
        EvacuationCenter center = activation.getCenter();

        return new EvacuationActivationResponse(
                activation.getId(),
                activation.getCurrentEvacuees(),
                activation.getStatus(),
                activation.getOpenedAt(),
                activation.getClosedAt(),
                activation.getIncident() != null ? activation.getIncident().getId() : null,
                activation.getCalamity() != null ? activation.getCalamity().getId() : null,
                center != null ? center.getId() : null,
                center != null ? center.getName() : null,
                center != null ? center.getCapacity() : null,
                center != null && center.getBarangay() != null ? center.getBarangay().getName() : null,
                center != null ? center.getLocationDetails() : null
        );
    }

}
