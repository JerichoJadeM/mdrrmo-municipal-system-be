package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.EvacuationCenter;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.EvacuationCenterRepository;
import com.isufst.mdrrmosystem.request.EvacuationCenterRequest;
import com.isufst.mdrrmosystem.response.EvacuationCenterResourceResponse;
import com.isufst.mdrrmosystem.response.EvacuationCenterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EvacuationCenterService {

    private final EvacuationCenterRepository evacuationCenterRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final BarangayRepository barangayRepository;

    public EvacuationCenterService(EvacuationCenterRepository evacuationCenterRepository,
                                   EvacuationActivationRepository evacuationActivationRepository,
                                   BarangayRepository barangayRepository) {
        this.evacuationCenterRepository = evacuationCenterRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.barangayRepository = barangayRepository;
    }

    @Transactional
    public EvacuationCenterResponse addEvacuationCenter(EvacuationCenterRequest request) {
        EvacuationCenter evacuationCenter = new EvacuationCenter();
        mapRequestToEntity(evacuationCenter, request);
        evacuationCenterRepository.save(evacuationCenter);
        return mapToResponse(evacuationCenter);
    }

    @Transactional
    public EvacuationCenterResponse updateEvacuationCenter(long centerId, EvacuationCenterRequest request) {
        EvacuationCenter center = evacuationCenterRepository.findById(centerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evacuation center not found: " + centerId));
        mapRequestToEntity(center, request);
        evacuationCenterRepository.save(center);
        return mapToResponse(center);
    }

    @Transactional(readOnly = true)
    public List<EvacuationCenterResponse> getCenterList() {
        return evacuationCenterRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvacuationCenterResourceResponse> getResourcesView(String keyword, String status, String usage) {
        List<EvacuationActivation> openActivations = evacuationActivationRepository.findByStatus("OPEN");

        return evacuationCenterRepository.findAll()
                .stream()
                .map(center -> {
                    int currentEvacuees = openActivations.stream()
                            .filter(a -> a.getCenter() != null && a.getCenter().getId() == (center.getId()))
                            .mapToInt(EvacuationActivation::getCurrentEvacuees)
                            .sum();

                    int capacity = Math.max(center.getCapacity(), 0);
                    int occupancyRate = capacity > 0 ? Math.min(100, Math.round((currentEvacuees * 100f) / capacity)) : 0;
                    int availableSlots = Math.max(capacity - currentEvacuees, 0);

                    String capacityStatus;
                    if (occupancyRate >= 95) {
                        capacityStatus = "FULL";
                    } else if (occupancyRate >= 80) {
                        capacityStatus = "NEAR_FULL";
                    } else {
                        capacityStatus = "AVAILABLE";
                    }

                    return new EvacuationCenterResourceResponse(
                            center.getId(),
                            center.getName(),
                            center.getBarangay() != null ? center.getBarangay().getId() : null,
                            center.getBarangay() != null ? center.getBarangay().getName() : null,
                            capacity,
                            currentEvacuees,
                            availableSlots,
                            occupancyRate,
                            capacityStatus,
                            center.getLocationDetails(),
                            center.getLatitude(),
                            center.getLongitude(),
                            center.getStatus()
                    );
                })
                .filter(c -> keyword == null || keyword.isBlank()
                        || (c.name() != null && c.name().toLowerCase().contains(keyword.toLowerCase()))
                        || (c.barangayName() != null && c.barangayName().toLowerCase().contains(keyword.toLowerCase())))
                .filter(c -> status == null || status.isBlank() || status.equalsIgnoreCase(c.status()))
                .filter(c -> usage == null || usage.isBlank() || usage.equalsIgnoreCase(c.capacityStatus()))
                .toList();
    }

    private void mapRequestToEntity(EvacuationCenter evacuationCenter, EvacuationCenterRequest request) {
        Barangay barangay = barangayRepository.findById(request.barangayId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barangay not found: " + request.barangayId()));

        evacuationCenter.setName(request.name());
        evacuationCenter.setBarangay(barangay);
        evacuationCenter.setCapacity(request.capacity());
        evacuationCenter.setLocationDetails(request.locationDetails());
        evacuationCenter.setLatitude(request.latitude());
        evacuationCenter.setLongitude(request.longitude());
        evacuationCenter.setStatus(request.status() != null && !request.status().isBlank()
                ? request.status().trim().toUpperCase()
                : "ACTIVE");
    }

    private EvacuationCenterResponse mapToResponse(EvacuationCenter c) {
        return new EvacuationCenterResponse(
                c.getId(),
                c.getName(),
                c.getBarangay() != null ? c.getBarangay().getId() : null,
                c.getBarangay() != null ? c.getBarangay().getName() : null,
                c.getCapacity(),
                c.getLocationDetails(),
                c.getLatitude(),
                c.getLongitude(),
                c.getStatus()
        );
    }
}
