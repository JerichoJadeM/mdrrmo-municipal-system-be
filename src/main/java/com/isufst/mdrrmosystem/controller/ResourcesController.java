package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.ResourcesReadinessSummaryResponse;
import com.isufst.mdrrmosystem.response.ResourcesSummaryResponse;
import com.isufst.mdrrmosystem.service.ResourcesReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class ResourcesController {

    private final ResourcesReadinessService resourceReadinessService;

    public ResourcesController(ResourcesReadinessService resourceReadinessService) {
        this.resourceReadinessService = resourceReadinessService;
    }

    @GetMapping("/summary")
    public ResourcesSummaryResponse getSummary() {
        return resourceReadinessService.getSummary();
    }

    @GetMapping("/readiness-summary")
    public ResourcesReadinessSummaryResponse getReadinessSummary() {
        return resourceReadinessService.getReadinessSummary();
    }
}