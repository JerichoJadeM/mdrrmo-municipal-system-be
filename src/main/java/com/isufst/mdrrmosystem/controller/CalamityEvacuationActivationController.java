package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.EvacuationActivationRequest;
import com.isufst.mdrrmosystem.request.UpdateEvacueesRequest;
import com.isufst.mdrrmosystem.response.EvacuationActivationResponse;
import com.isufst.mdrrmosystem.service.EvacuationActivationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calamities/{calamityId}/evacuations")
public class CalamityEvacuationActivationController {

    private final EvacuationActivationService evacuationActivationService;

    public CalamityEvacuationActivationController(EvacuationActivationService evacuationActivationService) {
        this.evacuationActivationService = evacuationActivationService;
    }

    @PostMapping
    public EvacuationActivationResponse activateCenter(@PathVariable Long calamityId,
                                                       @RequestBody @Valid EvacuationActivationRequest request) {
        return evacuationActivationService.activateCenterForCalamity(calamityId, request);
    }

    @GetMapping
    public List<EvacuationActivationResponse> getCalamityEvacuations(@PathVariable Long calamityId) {
        return evacuationActivationService.getByCalamity(calamityId);
    }

    @PutMapping("/{activationId}/evacuees")
    public EvacuationActivationResponse updateEvacuees(@PathVariable Long calamityId,
                                                       @PathVariable Long activationId,
                                                       @RequestBody @Valid UpdateEvacueesRequest request) {
        return evacuationActivationService.updateEvacueesForCalamity(calamityId, activationId, request);
    }

    @PutMapping("/{activationId}/close")
    public EvacuationActivationResponse closeCenter(@PathVariable Long calamityId,
                                                    @PathVariable Long activationId) {
        return evacuationActivationService.closeActivationForCalamity(calamityId, activationId);
    }
}
