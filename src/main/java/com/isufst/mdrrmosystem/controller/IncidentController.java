package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.DispatchIncidentRequest;
import com.isufst.mdrrmosystem.request.IncidentRequest;
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

    @GetMapping("/{id}/actions")
    public List<ResponseActionResponse> getIncidentActions(@PathVariable long id) {
        return incidentService.getIncidentsByActions(id);
    }

    @PutMapping("/{id}/resolve")
    public IncidentResponse resolveIncident(@PathVariable long id) {
        return incidentService.resolveIncident(id);
    }

    @PutMapping("/{id}/dispatch")
    public IncidentResponse dispatchIncident(@PathVariable long id, @RequestBody DispatchIncidentRequest incidentRequest) {
        return incidentService.dispatchResponder(id, incidentRequest);
    }

    @PutMapping("/{id}/arrive")
    public IncidentResponse markArrived(@PathVariable long id) {
        return incidentService.markResponderArrived(id);
    }
}
