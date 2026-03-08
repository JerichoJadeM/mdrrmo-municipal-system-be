package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.ResponseActionRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.DispatchIncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentRequest;
import com.isufst.mdrrmosystem.response.IncidentResponse;
import com.isufst.mdrrmosystem.response.ResponseActionResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final UserRepository userRepository;
    private final ResponseActionRepository responseActionRepository;
    private final BarangayRepository barangayRepository;

    public IncidentService(IncidentRepository incidentRepository,
                           FindAuthenticatedUser findAuthenticatedUser,
                           UserRepository userRepository,
                           ResponseActionRepository responseActionRepository,
                           BarangayRepository barangayRepository) {
        this.incidentRepository = incidentRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.userRepository = userRepository;
        this.responseActionRepository = responseActionRepository;
        this.barangayRepository = barangayRepository;
    }

    @Transactional
    public IncidentResponse newIncident(IncidentRequest incidentRequest) {
        Barangay barangay = barangayRepository.findById(incidentRequest.barangayId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barangay id not found"));

        validateBatadBarangay(barangay);

        User assignedResponder = null;
        if (incidentRequest.assignedResponderId() != null) {
            assignedResponder = userRepository.findById(incidentRequest.assignedResponderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned responder id not found"));
        }

        Incident incident = new Incident();
        incident.setType(incidentRequest.type().trim());
        incident.setBarangay(barangay);
        incident.setAssignedResponder(assignedResponder);
        incident.setSeverity(incidentRequest.severity().trim().toUpperCase());
        incident.setStatus("ONGOING");
        incident.setReportedAt(LocalDateTime.now());
        incident.setDescription(incidentRequest.description().trim());
        incident.setReportedBy(findAuthenticatedUser.getAuthenticatedUser());

        Incident savedIncident = incidentRepository.save(incident);

        if (assignedResponder != null) {
            ResponseAction action = new ResponseAction();
            action.setActionType("ASSIGN");
            action.setDescription("Responder "
                    + assignedResponder.getFirstName() + " "
                    + assignedResponder.getLastName()
                    + " assigned during incident creation.");
            action.setActionTime(LocalDateTime.now());
            action.setIncident(savedIncident);
            action.setResponder(assignedResponder);
            responseActionRepository.save(action);
        }

        return mapToResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public List<ResponseActionResponse> getIncidentsByActions(long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        return responseActionRepository.findByIncidentIdOrderByActionTimeDesc(incident.getId())
                .stream()
                .map(this::mapToResponseActionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll(Sort.by(Sort.Direction.DESC, "reportedAt"))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getActiveIncidentCount() {
        return incidentRepository.countByStatus("ONGOING");
    }

    @Transactional
    public IncidentResponse resolveIncident(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incident is already resolved");
        }

        if (!"ON_SITE".equalsIgnoreCase(incident.getStatus())
                && !"IN_PROGRESS".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only on-site or in-progress incidents can be resolved");
        }

        incident.setStatus("RESOLVED");
        incidentRepository.save(incident);

        ResponseAction action = new ResponseAction();
        action.setActionType("RESOLVE");
        action.setDescription("Incident marked as resolved.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);

        if (incident.getAssignedResponder() != null) {
            action.setResponder(incident.getAssignedResponder());
        }

        responseActionRepository.save(action);

        // Optional future logic:
        // if (incident.getAssignedResponder() != null) {
        //     User responder = incident.getAssignedResponder();
        //     responder.setAssignmentStatus("AVAILABLE");
        //     userRepository.save(responder);
        // }

        return mapToResponse(incident);
    }

    @Transactional
    public IncidentResponse dispatchResponder(long incidentId, DispatchIncidentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        User responder = userRepository.findById(request.responderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responder not found"));

        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolved incident cannot be dispatched");
        }

        incident.setAssignedResponder(responder);
        incident.setStatus("IN_PROGRESS");
        incidentRepository.save(incident);

        // Optional:
        // responder.setAssignmentStatus("BUSY");
        // userRepository.save(responder);

        ResponseAction action = new ResponseAction();
        action.setActionType("DISPATCH");
        action.setDescription("Responder " + responder.getFirstName() + " " + responder.getLastName() + " dispatched to scene");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(responder);

        responseActionRepository.save(action);

        return mapToResponse(incident);
    }

    @Transactional
    public IncidentResponse markResponderArrived(long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolved incident cannot be updated");
        }

        if (!"IN_PROGRESS".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only dispatched incidents can be marked as on-site");
        }

        if (incident.getAssignedResponder() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No responder assigned to this incident");
        }

        incident.setStatus("ON_SITE");
        incidentRepository.save(incident);

        ResponseAction action = new ResponseAction();
        action.setActionType("ARRIVAL");
        action.setDescription("Responder "
                + incident.getAssignedResponder().getFirstName() + " "
                + incident.getAssignedResponder().getLastName()
                + " arrived on site.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(incident.getAssignedResponder());

        responseActionRepository.save(action);

        return mapToResponse(incident);
    }

    private void validateBatadBarangay(Barangay barangay) {
        if (!"batad".equalsIgnoreCase(barangay.getMunicipalityName())
                || !"iloilo".equalsIgnoreCase(barangay.getProvinceName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Batad, Iloilo barangays are allowed");
        }
    }

    private ResponseActionResponse mapToResponseActionResponse(ResponseAction action) {
        return new ResponseActionResponse(
                action.getId(),
                action.getActionType(),
                action.getDescription(),
                action.getActionTime(),
                action.getIncident().getType(),
                action.getResponder() != null
                        ? action.getResponder().getFirstName() + " " + action.getResponder().getLastName()
                        : null
        );
    }

    private IncidentResponse mapToResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getType(),
                incident.getBarangay() != null ? incident.getBarangay().getName() : null,
                incident.getSeverity(),
                incident.getStatus(),
                incident.getReportedAt(),
                incident.getDescription(),
                incident.getAssignedResponder() != null ? incident.getAssignedResponder().getId() : null,
                incident.getAssignedResponder() != null ? incident.getAssignedResponder().getFullName() : null
        );
    }
}