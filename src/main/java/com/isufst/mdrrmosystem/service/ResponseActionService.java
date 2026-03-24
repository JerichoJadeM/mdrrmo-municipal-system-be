package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.ResponseActionRepository;
import com.isufst.mdrrmosystem.request.ResponseActionRequest;
import com.isufst.mdrrmosystem.response.ResponseActionResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public ResponseActionResponse addResponseAction(Long incidentId, ResponseActionRequest responseActionRequest) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("incident not found"));

        ResponseAction action = new ResponseAction();
        action.setActionType(responseActionRequest.actionType());
        action.setDescription(responseActionRequest.description());
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(findAuthenticatedUser.getAuthenticatedUser());
        // TODO and ask, is the responder the authenticated user?, or the people responded? if the latter, then responder are many not only one and should create a list of responders.

        responseActionRepository.save(action);

        return mapToResponse(action);
    }

    public List<ResponseActionResponse> getByIncident(Long incidentId) {
        return responseActionRepository.findByIncident_Id(incidentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ResponseActionResponse mapToResponse(ResponseAction action) {

        return new ResponseActionResponse(
                action.getId(),
                action.getActionType(),
                action.getDescription(),
                action.getActionTime(),
                action.getIncident().getType(),
                action.getResponder().getFirstName() + " " + action.getResponder().getLastName()
        );
    }
}
