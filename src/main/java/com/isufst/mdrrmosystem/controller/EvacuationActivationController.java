package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.EvacuationActivationRequest;
import com.isufst.mdrrmosystem.request.UpdateEvacueesRequest;
import com.isufst.mdrrmosystem.response.EvacuationActivationResponse;
import com.isufst.mdrrmosystem.service.EvacuationActivationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/evacuations")
public class EvacuationActivationController {

    private final EvacuationActivationService service;

    public EvacuationActivationController(EvacuationActivationService service) {
        this.service = service;
    }

    @PostMapping
    public EvacuationActivationResponse activate(
            @PathVariable Long incidentId,
            @RequestBody EvacuationActivationRequest request) {

        return service.activateCenter(incidentId, request);
    }

    @GetMapping
    public List<EvacuationActivationResponse> getAll(
            @PathVariable Long incidentId) {

        return service.getByIncident(incidentId);
    }

    @PutMapping("/{activationId}/evacuees")
    public EvacuationActivationResponse updateEvacuees(
            @PathVariable Long incidentId,
            @PathVariable Long activationId,
            @Valid @RequestBody UpdateEvacueesRequest request) {

        return service.updateEvacuees(incidentId, activationId, request);
    }

    @PutMapping("/{activationId}/close")
    public EvacuationActivationResponse close(
            @PathVariable Long activationId) {

        return service.closeCenter(activationId);
    }
}
