package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.RiskAssessment;
import com.isufst.mdrrmosystem.entity.WeatherData;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.RiskAssessmentRepository;
import com.isufst.mdrrmosystem.repository.WeatherDataRepository;
import com.isufst.mdrrmosystem.response.RiskLevelResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskEngineService {

    private final WeatherDataRepository weatherDataRepository;
    private final BarangayRepository barangayRepository;
    private final IncidentRepository incidentRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    public RiskEngineService(WeatherDataRepository weatherRepo,
                             BarangayRepository barangayRepo,
                             IncidentRepository incidentRepo,
                             RiskAssessmentRepository riskRepo){
        this.weatherDataRepository = weatherRepo;
        this.barangayRepository = barangayRepo;
        this.incidentRepository = incidentRepo;
        this.riskAssessmentRepository = riskRepo;

    }

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

    public void computeRisk(){
        WeatherData weather = weatherDataRepository.findTopByOrderByRecordedAtDesc();

        List<Barangay> barangays = barangayRepository.findAll();

        for(Barangay barangay : barangays){
            String riskLevel = "LOW";
            String reason = "Normal Conditions";

            // Rule 1 Rainfall
            if(weather.getRainfall() > 120 && barangay.isFloodProne()){
                riskLevel = "HIGH";
                reason = "Heavy rainfall and flood-prone area";

            } else if (weather.getRainfall() > 80) {
                riskLevel = "MEDIUM";
                reason = "Moderate rainfall";
            }

            // Rule 2 Active Incidents
            long incidents = incidentRepository.countByBarangayIdAndStatus(barangay.getId(), "ONGOING");

            if(incidents > 3){
                riskLevel = "HIGH";
                reason = "Multiple active incidents";
            }

            RiskAssessment riskA = new RiskAssessment(
                    barangay,
                    riskLevel,
                    reason,
                    LocalDateTime.now()
            );

            riskAssessmentRepository.save(riskA);
        }
    }
}
