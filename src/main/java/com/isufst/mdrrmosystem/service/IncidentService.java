package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.ResponseActionRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.DispatchIncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentRequest;
import com.isufst.mdrrmosystem.response.IncidentResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final UserRepository userRepository;
    private final ResponseActionRepository responseActionRepository;

    public IncidentService(IncidentRepository incidentRepository,  FindAuthenticatedUser findAuthenticatedUser,
                           UserRepository userRepository, ResponseActionRepository responseActionRepository) {
        this.incidentRepository = incidentRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.userRepository = userRepository;
        this.responseActionRepository = responseActionRepository;
    }

    @Transactional
    public IncidentResponse newIncident(IncidentRequest incidentRequest) {
        Incident incident = new Incident();

        incident.setType(incidentRequest.type);
        incident.setBarangay(incidentRequest.barangay);
        incident.setSeverity(incidentRequest.severity);
        incident.setDescription(incidentRequest.description);
        incident.setStatus("ONGOING");
        incident.setReportedAt(LocalDateTime.now());
        incident.setReportedBy(findAuthenticatedUser.getAuthenticatedUser());

        Incident saveNewIncident = incidentRepository.save(incident);

        return mapToResponse(saveNewIncident);

    }

    public List<IncidentResponse> getAllIncidents() {
       return incidentRepository.findAll()
               .stream()
               .map(this::mapToResponse)
               .toList();
    }

    public long getActiveIncidentCount() {
        return incidentRepository.countByStatus("ONGOING");
    }

    @Transactional
    public IncidentResponse resolveIncident(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("incident not found"));

        // validation
        if("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new RuntimeException("incident is already resolved");
        }

        // optional stricter lifecycle rule:
        // only allow resolution if responder is already arrived
        if(!"ON_SITE".equalsIgnoreCase(incident.getStatus())
                && !"IN_PROGRESS".equalsIgnoreCase(incident.getStatus())) {
            throw  new  RuntimeException("Only on-site incidents can be resolved");
        }

        //update status
        incident.setStatus("RESOLVED");
        incidentRepository.save(incident);

        // auto-log response action
        ResponseAction action = new ResponseAction();
        action.setActionType("RESOLVE");
        action.setDescription("Incident marked as resolved.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);

        // if responder exist, associate it in log
        if(incident.getAssignedResponder() != null) {
            action.setResponder(incident.getAssignedResponder());
        }

        responseActionRepository.save(action);

        // optional: reset responder availability
        if(incident.getAssignedResponder() != null) {
            User responder = incident.getAssignedResponder();

            // only if your User entity has this field
            // responder.setAvailabilityStatus("AVAILABLE");
            // userRepository.save(responder);
        }

        return mapToResponse(incident);
    }

    public IncidentResponse dispatchResponder(long incidentId, DispatchIncidentRequest request) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("incident not found"));

        User responder = userRepository.findById(request.responderId())
                .orElseThrow(() -> new RuntimeException("Responder not found"));

        // prevent dispatch if already resolved
        if("RESOLVE".equalsIgnoreCase(incident.getStatus())) {
           throw new RuntimeException("Resolved incident cannot be dispatched");
        }

        // assign responder
        incident.setAssignedResponder(responder);

        // update incident status
        incident.setStatus("IN_PROGRESS");
        incidentRepository.save(incident);

        // Todo:
        // optional: mark responder busy
        // only if User has availabilityStatus field
        // responder.setAvailabilityStatus("BUSY");
        // userRepository.save(responder);

        // auto create response log
        ResponseAction action = new ResponseAction();
        action.setActionType("DISPATCH");
        action.setDescription("RESPONDER " + responder.getUsername() + " dispatched to scene");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(responder);

        responseActionRepository.save(action);

        return mapToResponse(incident);

    }

    @Transactional
    public IncidentResponse markResponderArrived(long incidentId) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        // validate lifecycle
        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new RuntimeException("Resolved incident cannot be updated");
        }

        if (!"IN_PROGRESS".equalsIgnoreCase(incident.getStatus())) {
            throw new RuntimeException("Only dispatched incidents can be marked as on-site");
        }

        if (incident.getAssignedResponder() == null) {
            throw new RuntimeException("No responder assigned to this incident");
        }

        // update incident status
        incident.setStatus("ON_SITE");
        incidentRepository.save(incident);

        // auto-create response action log
        ResponseAction action = new ResponseAction();
        action.setActionType("ARRIVAL");
        action.setDescription("Responder "
                + incident.getAssignedResponder().getUsername()
                + " arrived on site.");
        action.setActionTime(LocalDateTime.now());
        action.setIncident(incident);
        action.setResponder(incident.getAssignedResponder());

        responseActionRepository.save(action);

        return mapToResponse(incident);
    }

    private IncidentResponse mapToResponse(Incident i) {

        return new IncidentResponse(
                i.getId(),
                i.getType(),
                i.getBarangay() != null ?i.getBarangay().getName() : null,
                i.getSeverity(),
                i.getStatus(),
                i.getReportedAt(),
                i.getDescription(),
                i.getAssignedResponder() != null
                        ? i.getAssignedResponder().getUsername()
                        : null
        );
    }
}
