package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.request.CalamityRequest;
import com.isufst.mdrrmosystem.response.CalamityResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CalamityService {

    private final CalamityRepository calamityRepository;

    public CalamityService(CalamityRepository calamityRepository) {
        this.calamityRepository = calamityRepository;
    }

    @Transactional
    public CalamityResponse addCalamityRecord(CalamityRequest calamityRequest) {

        Calamity calamity = new Calamity();

        calamity.setType(calamityRequest.type);
        calamity.setBarangay(calamityRequest.barangay);
        calamity.setSeverity(calamityRequest.severity);
        calamity.setDate(calamityRequest.date);
        calamity.setDamageCost(calamityRequest.damageCost);
        calamity.setCasualties(calamityRequest.casualties);
        calamity.setDescription(calamityRequest.description);

        return mapToResponse(calamityRepository.save(calamity));
    }

    public List<CalamityResponse> getAllCalamityRecords() {
        return calamityRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long getCalamitiesThisYear(){
        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end = LocalDate.now();

        return calamityRepository.countByDateBetween(start, end);
    }

    private CalamityResponse mapToResponse(Calamity c) {
        return new CalamityResponse(
                c.getId(),
                c.getType(),
                c.getBarangay(),
                c.getSeverity(),
                c.getDate(),
                c.getDamageCost(),
                c.getCasualties(),
                c.getDescription()
        );
    }
}
