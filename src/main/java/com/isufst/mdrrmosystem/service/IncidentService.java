package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.DispatchIncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentRequest;
import com.isufst.mdrrmosystem.response.IncidentResponse;
import com.isufst.mdrrmosystem.response.ResponseActionResponse;
import com.isufst.mdrrmosystem.response.WarningItem;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final OperationHistoryService operationHistoryService;
    private final NotificationService notificationService;
    private final OperationApprovalGuard operationApprovalGuard;

    public IncidentService(IncidentRepository incidentRepository,
                           FindAuthenticatedUser findAuthenticatedUser,
                           UserRepository userRepository,
                           ResponseActionRepository responseActionRepository,
                           BarangayRepository barangayRepository,
                           EvacuationActivationRepository evacuationActivationRepository,
                           ReliefDistributionRepository reliefDistributionRepository,
                           OperationHistoryService operationHistoryService,
                           NotificationService notificationService,
                           OperationApprovalGuard operationApprovalGuard) {
        this.incidentRepository = incidentRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.userRepository = userRepository;
        this.responseActionRepository = responseActionRepository;
        this.barangayRepository = barangayRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.operationHistoryService = operationHistoryService;
        this.notificationService = notificationService;
        this.operationApprovalGuard = operationApprovalGuard;
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

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                null,
                savedIncident.getStatus(),
                "Incident created with status ONGOING",
                null,
                null
        );

        notifyResponderIfAssigned(savedIncident, "You were assigned as responder for incident " + savedIncident.getType());
        notifyAllUsersIfHighOrCritical(savedIncident, "Incident marked HIGH/CRITICAL: " + savedIncident.getType());

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

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        operationApprovalGuard.validateOrThrowForIncidentTransition(
                actor,
                incident,
                "RESOLVED",
                buildIncidentWarnings(incident)
        );

        String oldStatus = incident.getStatus();
        incident.setStatus("RESOLVED");
        Incident savedIncident = incidentRepository.save(incident);

        ResponseAction action = new ResponseAction();
        action.setActionType("RESOLVE");
        action.setDescription("Incident marked as resolved.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);

        if (incident.getAssignedResponder() != null) {
            action.setResponder(incident.getAssignedResponder());
        }

        responseActionRepository.save(action);

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                oldStatus,
                savedIncident.getStatus(),
                "Incident moved to RESOLVED",
                null,
                null
        );

        return mapToResponse(savedIncident);
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
        String oldStatus = incident.getStatus();
        incident.setStatus("IN_PROGRESS");
        Incident savedIncident = incidentRepository.save(incident);

        ResponseAction action = new ResponseAction();
        action.setActionType("DISPATCH");
        action.setDescription("Responder " + responder.getFirstName() + " " + responder.getLastName() + " dispatched to scene");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(responder);

        responseActionRepository.save(action);

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                oldStatus,
                savedIncident.getStatus(),
                "Incident moved to IN_PROGRESS",
                null,
                null
        );

        notifyResponderIfAssigned(savedIncident, "You were dispatched to incident " + savedIncident.getType());

        return mapToResponse(savedIncident);
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

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        operationApprovalGuard.validateOrThrowForIncidentTransition(
                actor,
                incident,
                "ON_SITE",
                buildIncidentWarnings(incident)
        );

        String oldStatus = incident.getStatus();
        incident.setStatus("ON_SITE");
        Incident savedIncident = incidentRepository.save(incident);

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

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                oldStatus,
                savedIncident.getStatus(),
                "Incident moved to ON_SITE",
                null,
                null
        );

        return mapToResponse(savedIncident);
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
                incident.getBarangay() != null ? incident.getBarangay().getId() : null,
                incident.getBarangay() != null ? incident.getBarangay().getName() : null,
                incident.getSeverity(),
                incident.getStatus(),
                incident.getReportedAt(),
                incident.getDescription(),
                incident.getAssignedResponder() != null ? incident.getAssignedResponder().getId() : null,
                incident.getAssignedResponder() != null ? incident.getAssignedResponder().getFullName() : null
        );
    }

//  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public IncidentResponse updateIncident(long incidentId, IncidentRequest incidentRequest) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        Long oldResponderId = incident.getAssignedResponder() != null ? incident.getAssignedResponder().getId() : null;
        String oldSeverity = incident.getSeverity();

        Barangay barangay = null;
        if (incidentRequest.barangayId() != null) {
            barangay = barangayRepository.findById(incidentRequest.barangayId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barangay id not found"));
            validateBatadBarangay(barangay);
        }

        User assignedResponder = null;
        if (incidentRequest.assignedResponderId() != null) {
            assignedResponder = userRepository.findById(incidentRequest.assignedResponderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned responder not found"));
        }

        incident.setType(incidentRequest.type().trim());
        incident.setBarangay(barangay);
        incident.setAssignedResponder(assignedResponder);

        if (incidentRequest.severity() != null && !incidentRequest.severity().isBlank()) {
            incident.setSeverity(incidentRequest.severity().trim().toUpperCase());
        }

        if (incidentRequest.description() != null && !incidentRequest.description().isBlank()) {
            incident.setDescription(incidentRequest.description().trim());
        }

        Incident updated = incidentRepository.save(incident);

        Long newResponderId = updated.getAssignedResponder() != null ? updated.getAssignedResponder().getId() : null;
        if (newResponderId != null && !newResponderId.equals(oldResponderId)) {
            notifyResponderIfAssigned(updated, "You were assigned as responder for incident " + updated.getType());
        }

        if (isSeverityEscalatedToHighOrCritical(oldSeverity, updated.getSeverity())) {
            notifyAllUsersIfHighOrCritical(updated, "Incident escalated to HIGH/CRITICAL: " + updated.getType());
        }

        return mapToResponse(updated);
    }

//  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public void deleteIncident(long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        reliefDistributionRepository.deleteByIncidentId(incidentId);
        reliefDistributionRepository.flush();

        evacuationActivationRepository.deleteByIncidentId(incidentId);
        evacuationActivationRepository.flush();

        responseActionRepository.deleteAll(
                responseActionRepository.findByIncidentIdOrderByActionTimeDesc(incidentId)
        );
        responseActionRepository.flush();

        incidentRepository.delete(incident);
        incidentRepository.flush();
    }

    private void notifyResponderIfAssigned(Incident incident, String message) {
        if (incident.getAssignedResponder() == null) {
            return;
        }

        notificationService.notifyUser(
                incident.getAssignedResponder(),
                "ASSIGNMENT",
                "Incident Responder Assignment",
                message,
                "INCIDENT",
                incident.getId()
        );
    }

    private void notifyAllUsersIfHighOrCritical(Incident incident, String message) {
        if (!isHighOrCritical(incident.getSeverity())) {
            return;
        }

        notificationService.notifyAllUsers(
                "WARNING",
                "High/Critical Incident Alert",
                message,
                "INCIDENT",
                incident.getId()
        );
    }

    private boolean isHighOrCritical(String severity) {
        if (severity == null) return false;
        String value = severity.trim().toUpperCase();
        return "HIGH".equals(value) || "CRITICAL".equals(value);
    }

    private boolean isSeverityEscalatedToHighOrCritical(String oldSeverity, String newSeverity) {
        return !isHighOrCritical(oldSeverity) && isHighOrCritical(newSeverity);
    }

    private List<WarningItem> buildIncidentWarnings(Incident incident) {
        if (isHighOrCritical(incident.getSeverity())) {
            return List.of(
                    new WarningItem(
                            "WARNING",
                            "INCIDENT_HIGH_SEVERITY",
                            "High-severity incident transition requires acknowledgement.",
                            "Submit acknowledgement request or approve as manager/admin.",
                            true,
                            true
                    )
            );
        }
        return List.of();
    }
}