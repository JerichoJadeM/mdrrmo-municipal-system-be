package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.response.HeatmapResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private final IncidentRepository incidentRepository;

    public AnalyticsService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public List<HeatmapResponse> incidentHeatmap() {
        return incidentRepository.incidentHeatmap()
                .stream()
                .map(obj -> new HeatmapResponse(
                        (Long)obj[0],
                        (String)obj[1],
                        (Long)obj[2]
                ))
                .toList();
    }
}
