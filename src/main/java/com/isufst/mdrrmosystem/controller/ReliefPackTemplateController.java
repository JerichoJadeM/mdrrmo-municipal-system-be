package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.ReliefPackDistributionRequest;
import com.isufst.mdrrmosystem.request.ReliefPackTemplateRequest;
import com.isufst.mdrrmosystem.response.ActionSubmissionResponse;
import com.isufst.mdrrmosystem.response.ReliefPackReadinessResponse;
import com.isufst.mdrrmosystem.response.ReliefPackTemplateResponse;
import com.isufst.mdrrmosystem.service.ReliefPackTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relief-pack-templates")
public class ReliefPackTemplateController {

    private final ReliefPackTemplateService service;

    public ReliefPackTemplateController(ReliefPackTemplateService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ReliefPackTemplateResponse create(@RequestBody ReliefPackTemplateRequest request) {
        return service.createTemplate(request);
    }

    @GetMapping
    public List<ReliefPackTemplateResponse> getAll() {
        return service.getTemplates();
    }

    @GetMapping("/active")
    public List<ReliefPackTemplateResponse> getActive() {
        return service.getActiveTemplates();
    }

    @GetMapping("/{templateId}/readiness")
    public ReliefPackReadinessResponse getReadiness(@PathVariable Long templateId) {
        return service.getReadiness(templateId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{templateId}")
    public ReliefPackTemplateResponse update(@PathVariable Long templateId,
                                             @RequestBody ReliefPackTemplateRequest request) {
        return service.updateTemplate(templateId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{templateId}")
    public void delete(@PathVariable Long templateId) {
        service.deleteTemplate(templateId);
    }

    @PostMapping("/{templateId}/distribute/incidents/{incidentId}")
    public ActionSubmissionResponse distributeTemplateForIncident(@PathVariable Long templateId,
                                                                  @PathVariable Long incidentId,
                                                                  @RequestBody ReliefPackDistributionRequest request) {
        return service.distributeTemplateForIncident(
                templateId,
                incidentId,
                request.packCount(),
                request.evacuationActivationId()
        );
    }

    @PostMapping("/{templateId}/distribute/calamities/{calamityId}")
    public ActionSubmissionResponse distributeTemplateForCalamity(@PathVariable Long templateId,
                                                                  @PathVariable Long calamityId,
                                                                  @RequestBody ReliefPackDistributionRequest request) {
        return service.distributeTemplateForCalamity(
                templateId,
                calamityId,
                request.packCount(),
                request.evacuationActivationId()
        );
    }
}