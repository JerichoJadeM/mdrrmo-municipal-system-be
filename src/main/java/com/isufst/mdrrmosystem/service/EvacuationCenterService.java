package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.EvacuationCenter;
import com.isufst.mdrrmosystem.repository.EvacuationCenterRepository;
import com.isufst.mdrrmosystem.request.EvacuationCenterRequest;
import com.isufst.mdrrmosystem.response.EvacuationCenterResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EvacuationCenterService {

    private final EvacuationCenterRepository evacuationCenterRepository;

    public EvacuationCenterService(EvacuationCenterRepository evacuationCenterRepository) {
        this.evacuationCenterRepository = evacuationCenterRepository;
    }

    @Transactional
    public EvacuationCenterResponse addEvacuationCenter(EvacuationCenterRequest request) {
        EvacuationCenter evacuationCenter = new EvacuationCenter();

        evacuationCenter.setName(request.name());
        evacuationCenter.setBarangay(request.barangay());
        evacuationCenter.setCapacity(request.capacity());
        evacuationCenter.setLocationDetails(request.locationDetails());

        evacuationCenterRepository.save(evacuationCenter);

        return mapToResponse(evacuationCenter);
    }

    public List<EvacuationCenterResponse> getCenterList(){
        return evacuationCenterRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private EvacuationCenterResponse mapToResponse(EvacuationCenter c) {
        return new EvacuationCenterResponse(
                c.getId(),
                c.getName(),
                c.getBarangay(),
                c.getCapacity(),
                c.getLocationDetails()
        );
    }
}
