package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.ResponseActionRepository;
import com.isufst.mdrrmosystem.request.ResponseActionRequest;
import com.isufst.mdrrmosystem.response.ResponseActionResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResponseActionService {

    private final ResponseActionRepository responseActionRepository;
    private final IncidentRepository incidentRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    public ResponseActionService(ResponseActionRepository responseActionRepository,
                                 IncidentRepository incidentRepository,
                                 FindAuthenticatedUser findAuthenticatedUser) {
        this.responseActionRepository = responseActionRepository;
        this.incidentRepository = incidentRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Transactional
    public ResponseActionResponse addResponseAction(Long incidentId, ResponseActionRequest request) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        if (incident.getAssignedResponder() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add a response action because this incident has no assigned responder.");
        }

        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add a response action to a resolved incident.");
        }

        User assignedResponder = incident.getAssignedResponder();
        User actor = findAuthenticatedUser.getAuthenticatedUser();

        // 🔒 Permission check (recommended)
        boolean isAssignedResponder = actor != null && actor.getId().equals(assignedResponder.getId());
        boolean isPrivileged = actor != null &&
                actor.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                                || a.getAuthority().equals("ROLE_MANAGER"));

        if (!isAssignedResponder && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to log this response action.");
        }

        // 🧠 Prevent duplicate spam actions
        Optional<ResponseAction> latest = responseActionRepository
                .findTopByIncidentIdOrderByActionTimeDesc(incidentId);

        if (latest.isPresent()) {
            ResponseAction last = latest.get();
            if (last.getActionType().equalsIgnoreCase(request.actionType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate action: " + request.actionType());
            }
        }

        ResponseAction action = new ResponseAction();
        action.setActionType(normalizeActionType(request.actionType()));
        action.setDescription(normalizeDescription(request.description()));
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(assignedResponder); // ✅ ALWAYS source of truth

        responseActionRepository.save(action);

        return mapToResponse(action);
    }

    @Transactional(readOnly = true)
    public List<ResponseActionResponse> getByIncident(Long incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        return responseActionRepository.findByIncidentIdOrderByActionTimeDesc(incidentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private String normalizeActionType(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action type is required.");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return "No description provided.";
        }
        return value.trim();
    }

    private ResponseActionResponse mapToResponse(ResponseAction action) {
        return new ResponseActionResponse(
                action.getId(),
                action.getActionType(),
                action.getDescription(),
                action.getActionTime(),
                action.getIncident() != null ? action.getIncident().getType() : "--",
                action.getResponder() != null
                        ? (action.getResponder().getFirstName() + " " + action.getResponder().getLastName()).trim()
                        : "--"
        );
    }
}