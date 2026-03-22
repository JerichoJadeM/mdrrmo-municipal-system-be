package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.OperationReliefStatusResponse;
import com.isufst.mdrrmosystem.service.OperationReliefStatusService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations")
public class OperationReliefStatusController {

    private final OperationReliefStatusService operationReliefStatusService;

    public OperationReliefStatusController(OperationReliefStatusService operationReliefStatusService) {
        this.operationReliefStatusService = operationReliefStatusService;
    }

    @GetMapping("/incidents/{incidentId}/relief-status")
    public OperationReliefStatusResponse getIncidentReliefStatus(@PathVariable Long incidentId) {
        return operationReliefStatusService.getIncidentReliefStatus(incidentId);
    }

    @GetMapping("/calamities/{calamityId}/relief-status")
    public OperationReliefStatusResponse getCalamityReliefStatus(@PathVariable Long calamityId) {
        return operationReliefStatusService.getCalamityReliefStatus(calamityId);
    }
}