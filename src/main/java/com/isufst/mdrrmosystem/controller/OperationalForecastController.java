package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.OperationalForecastResponse;
import com.isufst.mdrrmosystem.service.OperationalForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations/forecast")
public class OperationalForecastController {

    private final OperationalForecastService operationalForecastService;

    public OperationalForecastController(OperationalForecastService operationalForecastService) {
        this.operationalForecastService = operationalForecastService;
    }

    @GetMapping("/incidents/{incidentId}")
    public OperationalForecastResponse forecastIncident(@PathVariable Long incidentId) {
        return operationalForecastService.forecastIncident(incidentId);
    }

    @GetMapping("/calamities/{calamityId}")
    public OperationalForecastResponse forecastCalamity(@PathVariable Long calamityId) {
        return operationalForecastService.forecastCalamity(calamityId);
    }
}
