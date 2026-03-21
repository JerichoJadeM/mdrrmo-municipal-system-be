package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.ReliefDistributionRequest;
import com.isufst.mdrrmosystem.response.ReliefDistributionResponse;
import com.isufst.mdrrmosystem.service.ReliefDistributionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReliefDistributionController {

    private final ReliefDistributionService service;

    public ReliefDistributionController(ReliefDistributionService service) {
        this.service = service;
    }

    @PostMapping("/incidents/{incidentId}/relief")
    public ReliefDistributionResponse distribute(
            @PathVariable Long incidentId,
            @RequestBody ReliefDistributionRequest request) {

        return service.distribute(incidentId, request);
    }

    @GetMapping("/incidents/{incidentId}/relief")
    public List<ReliefDistributionResponse> getAll(
            @PathVariable Long incidentId) {

        return service.getByIncident(incidentId);
    }

    @PostMapping("/calamity/{calamityId}/relief")
    public ReliefDistributionResponse distributeCalamity(
            @PathVariable Long calamityId,
            @RequestBody ReliefDistributionRequest request) {
        return service.distributeForCalamity(calamityId, request);
    }

    @GetMapping("/calamity/{calamityId}/relief")
    public List<ReliefDistributionResponse> getAllCalamity(@PathVariable Long calamityId) {
        return service.getByCalamity(calamityId);
    }
}
