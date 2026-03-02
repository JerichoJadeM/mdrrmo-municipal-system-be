package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.response.RiskLevelResponse;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineService {

    public RiskLevelResponse computeRisk(String barangay, double rainfall){

        String risk;
        String recommendation;

        if(rainfall > 200){
            risk = "HIGH";
            recommendation = "Prepare evacuation centers";
        } else if(rainfall > 100){
            risk = "MEDIUM";
            recommendation = "Monitor river levels";
        } else {
            risk = "LOW";
            recommendation = "Normal Monitoring";
        }

        return new RiskLevelResponse(
                barangay,
                risk,
                recommendation
        );
    }
}
