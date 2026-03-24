package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.AlertsWarningsOverviewResponse;
import com.isufst.mdrrmosystem.service.AlertsWarningsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts-warnings")
public class AlertsWarningsController {

    private final AlertsWarningsService alertsWarningsService;

    public AlertsWarningsController(AlertsWarningsService alertsWarningsService) {
        this.alertsWarningsService = alertsWarningsService;
    }

    @GetMapping("/overview")
    public AlertsWarningsOverviewResponse getOverview() {
        return alertsWarningsService.getOverview();
    }
}
