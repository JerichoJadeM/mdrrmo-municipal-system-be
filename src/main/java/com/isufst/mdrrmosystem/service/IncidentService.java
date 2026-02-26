package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.request.IncidentRequest;
import com.isufst.mdrrmosystem.response.IncidentResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    public IncidentService(IncidentRepository incidentRepository,  FindAuthenticatedUser findAuthenticatedUser) {
        this.incidentRepository = incidentRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Transactional
    public IncidentResponse newIncident(IncidentRequest incidentRequest) {
        Incident incident = new Incident();

        incident.setType(incidentRequest.type);
        incident.setBarangay(incidentRequest.barangay);
        incident.setSeverity(incidentRequest.severity);
        incident.setDescription(incidentRequest.description);
        incident.setStatus("ONGOING");
        incident.setReportedAt(LocalDateTime.now());
        incident.setReportedBy(findAuthenticatedUser.getAuthenticatedUser());

        Incident saveNewIncident = incidentRepository.save(incident);

        return mapToResponse(saveNewIncident);

    }

    public List<IncidentResponse> getAllIncidents() {
       return incidentRepository.findAll()
               .stream()
               .map(this::mapToResponse)
               .toList();
    }

    public long getActiveIncidentCount() {
        return incidentRepository.countByStatus("ONGOING");
    }

    @Transactional
    public IncidentResponse resolveIncident(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow();

        incident.setStatus("RESOLVED");

        return mapToResponse(incidentRepository.save(incident));
    }

    private IncidentResponse mapToResponse(Incident i) {

        return new IncidentResponse(
                i.getId(),
                i.getType(),
                i.getBarangay(),
                i.getSeverity(),
                i.getStatus(),
                i.getReportedAt(),
                i.getDescription()
        );
    }
}
