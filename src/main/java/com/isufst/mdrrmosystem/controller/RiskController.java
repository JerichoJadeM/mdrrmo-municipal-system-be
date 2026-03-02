package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.RiskLevelResponse;
import com.isufst.mdrrmosystem.service.RiskEngineService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskEngineService riskEngineService;

    public RiskController(RiskEngineService riskEngineService) {
        this.riskEngineService = riskEngineService;
    }

    public RiskLevelResponse getRiskLevel(
            @RequestParam String barangay,
            @RequestParam double rainfall
    ) {
        return riskEngineService.computeRisk(barangay, rainfall);
    }

    @PostMapping("/compute")
    public String computeRisk(){
        riskEngineService.computeRisk();

        return "Risk Computation Completed";
    }

}
