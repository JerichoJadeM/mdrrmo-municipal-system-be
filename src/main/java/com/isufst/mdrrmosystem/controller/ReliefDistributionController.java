package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.ReliefDistributionRequest;
import com.isufst.mdrrmosystem.response.ReliefDistributionResponse;
import com.isufst.mdrrmosystem.service.ReliefDistributionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/relief")
public class ReliefDistributionController {

    private final ReliefDistributionService service;

    public ReliefDistributionController(ReliefDistributionService service) {
        this.service = service;
    }

    @PostMapping
    public ReliefDistributionResponse distribute(
            @PathVariable Long incidentId,
            @RequestBody ReliefDistributionRequest request) {

        return service.distribute(incidentId, request);
    }

    @GetMapping
    public List<ReliefDistributionResponse> getAll(
            @PathVariable Long incidentId) {

        return service.getByIncident(incidentId);
    }
}
