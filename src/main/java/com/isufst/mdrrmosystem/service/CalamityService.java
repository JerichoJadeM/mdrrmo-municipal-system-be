package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.CalamityRequest;
import com.isufst.mdrrmosystem.response.CalamityResponse;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class CalamityService {

    private final CalamityRepository calamityRepository;
    private final BarangayRepository barangayRepository;
    private final UserRepository userRepository;

    public CalamityService(CalamityRepository calamityRepository,
                           BarangayRepository barangayRepository,
                           UserRepository userRepository) {
        this.calamityRepository = calamityRepository;
        this.barangayRepository = barangayRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CalamityResponse addCalamityRecord(CalamityRequest calamityRequest) {
        Barangay barangay = barangayRepository.findById(calamityRequest.barangayId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barangay id not found"));

        validateBatadBarangay(barangay);

        User coordinator = null;
        if (calamityRequest.coordinatorId() != null) {
            coordinator = userRepository.findById(calamityRequest.coordinatorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coordinator id not found"));
        }

        Calamity calamity = new Calamity();
        calamity.setType(calamityRequest.type().trim());
        calamity.setStatus("ACTIVE");
        calamity.setBarangay(barangay);
        calamity.setCoordinator(coordinator);
        calamity.setSeverity(calamityRequest.severity().trim().toUpperCase());
        calamity.setDate(calamityRequest.date());
        calamity.setDamageCost(calamityRequest.damageCost());
        calamity.setCasualties(calamityRequest.casualties());
        calamity.setDescription(calamityRequest.description().trim());

        Calamity savedCalamity = calamityRepository.save(calamity);
        return mapToResponse(savedCalamity);
    }

    @Transactional(readOnly = true)
    public List<CalamityResponse> getAllCalamityRecords() {
        return calamityRepository.findAll(Sort.by(Sort.Direction.DESC, "date"))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getCalamitiesThisYear() {
        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end = LocalDate.now();

        return calamityRepository.countByDateBetween(start, end);
    }

    private void validateBatadBarangay(Barangay barangay) {
        if (!"batad".equalsIgnoreCase(barangay.getMunicipalityName())
                || !"iloilo".equalsIgnoreCase(barangay.getProvinceName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Batad, Iloilo barangays are allowed");
        }
    }

    private CalamityResponse mapToResponse(Calamity calamity) {
        return new CalamityResponse(
                calamity.getId(),
                calamity.getType(),
                calamity.getStatus(),
                calamity.getBarangay() != null ? calamity.getBarangay().getName() : null,
                calamity.getSeverity(),
                calamity.getDate(),
                calamity.getDamageCost(),
                calamity.getCasualties(),
                calamity.getDescription(),
                calamity.getCoordinator() != null ? calamity.getCoordinator().getId() : null,
                calamity.getCoordinator() != null ? calamity.getCoordinator().getFullName() : null
        );
    }
}