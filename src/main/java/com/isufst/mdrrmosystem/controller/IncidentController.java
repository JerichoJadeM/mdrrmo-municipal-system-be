package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.DispatchIncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentTransitionRequest;
import com.isufst.mdrrmosystem.response.IncidentResponse;
import com.isufst.mdrrmosystem.response.ResponseActionResponse;
import com.isufst.mdrrmosystem.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public IncidentResponse newIncident(@Valid @RequestBody IncidentRequest incidentRequest) {
        return incidentService.newIncident(incidentRequest);
    }

    @GetMapping
    public List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/archived")
    public List<IncidentResponse> getArchivedIncidents() {
        return incidentService.getArchivedIncidents();
    }

    @GetMapping("/{id}/actions")
    public List<ResponseActionResponse> getIncidentActions(@PathVariable long id) {
        return incidentService.getIncidentsByActions(id);
    }

    @PutMapping("/{id}/resolve")
    public IncidentResponse resolveIncident(@PathVariable long id,
                                            @RequestBody(required = false) IncidentTransitionRequest request) {
        return incidentService.resolveIncident(id, request);
    }

    @PutMapping("/{id}/dispatch")
    public IncidentResponse dispatchIncident(@PathVariable long id, @RequestBody DispatchIncidentRequest incidentRequest) {
        return incidentService.dispatchResponder(id, incidentRequest);
    }

    @PutMapping("/{id}/arrive")
    public IncidentResponse markArrived(@PathVariable long id,
                                        @RequestBody(required = false) IncidentTransitionRequest request) {
        return incidentService.markResponderArrived(id, request);
    }

    @PutMapping("/{id}")
    public IncidentResponse updateIncident(
            @PathVariable long id,
            @Valid @RequestBody IncidentRequest incidentRequest
    ) {
        return incidentService.updateIncident(id, incidentRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteIncident(@PathVariable long id) {
        incidentService.deleteIncident(id);
    }

    @PutMapping("/{id}/archive")
    public IncidentResponse archiveIncident(@PathVariable long id) {
        return incidentService.archiveIncident(id);
    }

    @PutMapping("/{id}/restore")
    public IncidentResponse restoreIncident(@PathVariable long id) {
        return incidentService.restoreIncident(id);
    }

    @PutMapping("/{id}/archive/clear")
    public IncidentResponse clearArchivedIncident(@PathVariable long id) {
        return incidentService.clearArchivedIncident(id);
    }

    @PutMapping("/archived/restore-all")
    public List<IncidentResponse> restoreAllArchivedIncidents() {
        return incidentService.restoreAllArchivedIncidents();
    }

    @PutMapping("/archived/clear-all")
    public List<IncidentResponse> clearAllArchivedIncidents() {
        return incidentService.clearAllArchivedIncidents();
    }
}


