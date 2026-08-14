package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.request.DispatchIncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentTransitionRequest;
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
    private final OperationResourceUsageService operationResourceUsageService;

    public IncidentService(IncidentRepository incidentRepository,
                           FindAuthenticatedUser findAuthenticatedUser,
                           UserRepository userRepository,
                           ResponseActionRepository responseActionRepository,
                           BarangayRepository barangayRepository,
                           EvacuationActivationRepository evacuationActivationRepository,
                           ReliefDistributionRepository reliefDistributionRepository,
                           OperationHistoryService operationHistoryService,
                           NotificationService notificationService,
                           OperationApprovalGuard operationApprovalGuard,
                           OperationResourceUsageService operationResourceUsageService) {
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
        this.operationResourceUsageService = operationResourceUsageService;
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
            validateResponderAssignable(assignedResponder);
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
            markResponderBusy(assignedResponder);

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
    public IncidentResponse resolveIncident(long id, IncidentTransitionRequest request) {
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

        User responderToRelease = incident.getAssignedResponder();

        String oldStatus = incident.getStatus();
        incident.setStatus("RESOLVED");
        Incident savedIncident = incidentRepository.save(incident);

        ResponseAction action = new ResponseAction();
        action.setActionType("RESOLVE");
        action.setDescription(hasText(request != null ? request.description() : null)
                ? request.description().trim()
                : "Incident marked as resolved.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);

        if (responderToRelease != null) {
            action.setResponder(responderToRelease);
        }

        responseActionRepository.save(action);

        double actualCost = operationResourceUsageService.recordUsage(
                "INCIDENT", savedIncident.getId(), "RESOLVE",
                request != null ? request.selectedResources() : null, actor);

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                oldStatus,
                savedIncident.getStatus(),
                "Incident moved to RESOLVED",
                buildTransitionMetadata(request != null ? request.overrideReason() : null, actualCost),
                actor != null ? actor.getFullName() : null
        );

        releaseResponderIfNoOtherActiveIncidents(responderToRelease, incident.getId());

        return mapToResponse(savedIncident);
    }

    @Transactional
    public IncidentResponse dispatchResponder(long incidentId, DispatchIncidentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        User previousResponder = incident.getAssignedResponder();

        User responder = userRepository.findById(request.responderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responder not found"));

        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolved incident cannot be dispatched");
        }

        if (!isSameResponder(previousResponder, responder)) {
            validateResponderAssignable(responder);
        }

        incident.setAssignedResponder(responder);
        String oldStatus = incident.getStatus();
        incident.setStatus("IN_PROGRESS");
        Incident savedIncident = incidentRepository.save(incident);

        if (!isSameResponder(previousResponder, responder)) {
            markResponderBusy(responder);
            releaseResponderIfNoOtherActiveIncidents(previousResponder, incident.getId());
        } else if (responder != null) {
            markResponderBusy(responder);
        }

        ResponseAction action = new ResponseAction();
        action.setActionType("DISPATCH");
        action.setDescription(hasText(request.description())
                ? request.description().trim()
                : "Responder " + responder.getFirstName() + " " + responder.getLastName() + " dispatched to scene");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(responder);
        responseActionRepository.save(action);

        User dispatchActor = findAuthenticatedUser.getAuthenticatedUser();
        double actualCost = operationResourceUsageService.recordUsage(
                "INCIDENT", savedIncident.getId(), "DISPATCH", request.selectedResources(), dispatchActor);

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                oldStatus,
                savedIncident.getStatus(),
                "Incident moved to IN_PROGRESS",
                buildTransitionMetadata(request.overrideReason(), actualCost),
                dispatchActor != null ? dispatchActor.getFullName() : null
        );

        notifyResponderIfAssigned(savedIncident, "You were dispatched to incident " + savedIncident.getType());

        return mapToResponse(savedIncident);
    }

    @Transactional
    public IncidentResponse markResponderArrived(long incidentId, IncidentTransitionRequest request) {
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
        action.setDescription(hasText(request != null ? request.description() : null)
                ? request.description().trim()
                : "Responder "
                        + incident.getAssignedResponder().getFirstName() + " "
                        + incident.getAssignedResponder().getLastName()
                        + " arrived on site.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(incident.getAssignedResponder());

        responseActionRepository.save(action);

        double actualCost = operationResourceUsageService.recordUsage(
                "INCIDENT", savedIncident.getId(), "ARRIVE",
                request != null ? request.selectedResources() : null, actor);

        operationHistoryService.log(
                "INCIDENT",
                savedIncident.getId(),
                "STATUS_CHANGED",
                oldStatus,
                savedIncident.getStatus(),
                "Incident moved to ON_SITE",
                buildTransitionMetadata(request != null ? request.overrideReason() : null, actualCost),
                actor != null ? actor.getFullName() : null
        );

        return mapToResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getArchivedIncidents() {
        return incidentRepository.findByStatusAndArchiveClearedAtIsNull("ARCHIVED")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public IncidentResponse archiveIncident(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if (!"RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only resolved incidents can be archived");
        }

        String oldStatus = incident.getStatus();
        incident.setStatus("ARCHIVED");
        incident.setArchivedAt(LocalDateTime.now());
        incident.setArchiveClearedAt(null);
        Incident saved = incidentRepository.save(incident);

        logArchiveHistory(saved.getId(), oldStatus, saved.getStatus(), "Incident archived");

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse restoreIncident(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if (!"ARCHIVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only archived incidents can be restored");
        }

        String oldStatus = incident.getStatus();
        incident.setStatus("RESOLVED");
        incident.setArchivedAt(null);
        incident.setArchiveClearedAt(null);
        Incident saved = incidentRepository.save(incident);

        logArchiveHistory(saved.getId(), oldStatus, saved.getStatus(), "Incident restored from archive");

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse clearArchivedIncident(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if (!"ARCHIVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only archived incidents can be cleared");
        }

        incident.setArchiveClearedAt(LocalDateTime.now());
        Incident saved = incidentRepository.save(incident);

        logArchiveHistory(saved.getId(), saved.getStatus(), saved.getStatus(), "Archived incident cleared from view");

        return mapToResponse(saved);
    }

    @Transactional
    public List<IncidentResponse> restoreAllArchivedIncidents() {
        List<Incident> archived = incidentRepository.findByStatusAndArchiveClearedAtIsNull("ARCHIVED");

        List<Incident> restored = archived.stream()
                .map(incident -> {
                    String oldStatus = incident.getStatus();
                    incident.setStatus("RESOLVED");
                    incident.setArchivedAt(null);
                    incident.setArchiveClearedAt(null);
                    Incident saved = incidentRepository.save(incident);
                    logArchiveHistory(saved.getId(), oldStatus, saved.getStatus(), "Incident restored from archive (bulk)");
                    return saved;
                })
                .toList();

        return restored.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public List<IncidentResponse> clearAllArchivedIncidents() {
        List<Incident> archived = incidentRepository.findByStatusAndArchiveClearedAtIsNull("ARCHIVED");

        List<Incident> cleared = archived.stream()
                .map(incident -> {
                    incident.setArchiveClearedAt(LocalDateTime.now());
                    Incident saved = incidentRepository.save(incident);
                    logArchiveHistory(saved.getId(), saved.getStatus(), saved.getStatus(), "Archived incident cleared from view (bulk)");
                    return saved;
                })
                .toList();

        return cleared.stream().map(this::mapToResponse).toList();
    }

    private void logArchiveHistory(long incidentId, String oldStatus, String newStatus, String description) {
        User actor = findAuthenticatedUser.getAuthenticatedUser();
        operationHistoryService.log(
                "INCIDENT",
                incidentId,
                "STATUS_CHANGED",
                oldStatus,
                newStatus,
                description,
                null,
                actor != null ? actor.getFullName() : null
        );
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

        User oldResponder = incident.getAssignedResponder();
        Long oldResponderId = oldResponder != null ? oldResponder.getId() : null;
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

            if (oldResponderId == null || !oldResponderId.equals(assignedResponder.getId())) {
                validateResponderAssignable(assignedResponder);
            }
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
            markResponderBusy(updated.getAssignedResponder());
            releaseResponderIfNoOtherActiveIncidents(oldResponder, incident.getId());
            notifyResponderIfAssigned(updated, "You were assigned as responder for incident " + updated.getType());
        }

        if (newResponderId == null && oldResponderId != null) {
            releaseResponderIfNoOtherActiveIncidents(oldResponder, incident.getId());
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

        User responderToRelease = incident.getAssignedResponder();

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

        releaseResponderIfNoOtherActiveIncidents(responderToRelease, incidentId);
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

    private void validateResponderAssignable(User responder) {
        if (responder == null) return;

        if (!"ACTIVE".equalsIgnoreCase(responder.getAccountStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned responder is not active.");
        }

        if (!Boolean.TRUE.equals(responder.getResponderEligible())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not responder-eligible.");
        }

        if (!"AVAILABLE".equalsIgnoreCase(responder.getAssignmentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned responder is not currently available.");
        }
    }

    private void markResponderBusy(User responder) {
        if (responder == null) return;

        responder.setAssignmentStatus("BUSY");
        userRepository.save(responder);
    }

    private void releaseResponderIfNoOtherActiveIncidents(User responder, Long currentIncidentId) {
        if (responder == null || responder.getId() == null) return;

        long otherActiveAssignments = currentIncidentId == null
                ? incidentRepository.countActiveAssignmentsByResponderId(responder.getId())
                : incidentRepository.countActiveAssignmentsByResponderIdAndIdNot(responder.getId(), currentIncidentId);

        if (otherActiveAssignments <= 0) {
            responder.setAssignmentStatus("AVAILABLE");
            userRepository.save(responder);
        }
    }

    private boolean isSameResponder(User left, User right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildTransitionMetadata(String overrideReason, double actualCost) {
        boolean hasOverrideReason = hasText(overrideReason);
        if (!hasOverrideReason && actualCost <= 0) {
            return null;
        }

        StringBuilder json = new StringBuilder("{");
        boolean appended = false;
        if (hasOverrideReason) {
            json.append("\"overrideReason\":\"").append(escapeJson(overrideReason.trim())).append("\"");
            appended = true;
        }
        if (actualCost > 0) {
            if (appended) {
                json.append(",");
            }
            json.append("\"actualCost\":").append(actualCost);
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void logResponseAction(Incident incident, User responder, String actionType, String description) {
        if (incident == null || responder == null) return;

        ResponseAction action = new ResponseAction();
        action.setActionType(actionType);
        action.setDescription(description);
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(responder);
        responseActionRepository.save(action);
    }
}