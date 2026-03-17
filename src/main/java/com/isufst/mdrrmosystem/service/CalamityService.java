package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.CalamityRequest;
import com.isufst.mdrrmosystem.request.CalamityTransitionRequest;
import com.isufst.mdrrmosystem.response.CalamityResponse;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalamityService {

    private final CalamityRepository calamityRepository;
    private final BarangayRepository barangayRepository;
    private final UserRepository userRepository;
    private final OperationHistoryService operationHistoryService;

    public CalamityService(CalamityRepository calamityRepository,
                           BarangayRepository barangayRepository,
                           UserRepository userRepository,
                           OperationHistoryService operationHistoryService) {
        this.calamityRepository = calamityRepository;
        this.barangayRepository = barangayRepository;
        this.userRepository = userRepository;
        this.operationHistoryService = operationHistoryService;
    }

    @Transactional
    public CalamityResponse addCalamityRecord(CalamityRequest calamityRequest) {
        Calamity calamity = new Calamity();
        calamity.setStatus("ACTIVE"); // Default status for new records

        // Pass the new object to the shared logic
        mapRequestToEntity(calamity, calamityRequest);

        Calamity savedCalamity = calamityRepository.save(calamity);
        return mapToResponse(savedCalamity);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public CalamityResponse updateCalamityRecord(long calamityId, CalamityRequest calamityRequest) {
        // 1. Find existing record
        Calamity existingCalamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        // 2. Use the shared logic to update fields (avoids code duplication)
        mapRequestToEntity(existingCalamity, calamityRequest);

        // 3. Save and return
        Calamity updatedCalamity = calamityRepository.save(existingCalamity);
        return mapToResponse(updatedCalamity);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public CalamityResponse markCalamityMonitoring(long calamityId, CalamityTransitionRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        if (!"ACTIVE".equalsIgnoreCase(calamity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only ACTIVE calamities can be moved to MONITORING");
        }

        applyTransitionUpdates(calamity, request);
        String oldStatus = calamity.getStatus();
        calamity.setStatus("MONITORING");
        Calamity saved = calamityRepository.save(calamity);

        operationHistoryService.log(
                "CALAMITY",
                saved.getId(),
                "STATUS_CHANGED",
                oldStatus,
                saved.getStatus(),
                "Calamity moved to MONITORING",
                null,
                null
        );

        return mapToResponse(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public CalamityResponse markCalamityResolved(long calamityId, CalamityTransitionRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        if (!"ACTIVE".equalsIgnoreCase(calamity.getStatus())
                && !"MONITORING".equalsIgnoreCase(calamity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only ACTIVE or MONITORING calamities can be moved to RESOLVED");
        }

        applyTransitionUpdates(calamity, request);

        String oldStatus = calamity.getStatus();
        calamity.setStatus("RESOLVED");
        Calamity saved = calamityRepository.save(calamity);

        operationHistoryService.log(
                "CALAMITY",
                saved.getId(),
                "STATUS_CHANGED",
                oldStatus,
                saved.getStatus(),
                "Calamity moved to RESOLVED",
                null,
                null
        );

        return mapToResponse(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public CalamityResponse markCalamityEnded(long calamityId, CalamityTransitionRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        if (!"RESOLVED".equalsIgnoreCase(calamity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only RESOLVED calamities can be moved to ENDED");
        }

        applyTransitionUpdates(calamity, request);

        String oldStatus = calamity.getStatus();
        calamity.setStatus("ENDED");
        Calamity saved = calamityRepository.save(calamity);

        operationHistoryService.log(
                "CALAMITY",
                saved.getId(),
                "STATUS_CHANGED",
                oldStatus,
                saved.getStatus(),
                "Calamity moved to ENDED",
                null,
                null
        );

        return mapToResponse(saved);
    }

    private void applyTransitionUpdates(Calamity calamity, CalamityTransitionRequest request) {
        if (request != null && request.description() != null && !request.description().isBlank()) {
            calamity.setDescription(request.description().trim());
        }
    }

    /**
     * Shared logic for both Create and Update operations.
     * This ensures validations are consistent across the system.
     */
    private void mapRequestToEntity(Calamity calamity, CalamityRequest calamityRequest) {
        User coordinator = null;
        if (calamityRequest.coordinatorId() != null) {
            coordinator = userRepository.findById(calamityRequest.coordinatorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Coordinator id not found"));
        }

        String affectedAreaType = calamityRequest.affectedAreaType().trim().toUpperCase();

        calamity.setType(calamityRequest.type().trim());
        calamity.setEventName(calamityRequest.eventName() != null && !calamityRequest.eventName().isBlank()
                ? calamityRequest.eventName().trim()
                : null);
        calamity.setAffectedAreaTypes(affectedAreaType);
        calamity.setCoordinator(coordinator);
        calamity.setSeverity(calamityRequest.severity().trim().toUpperCase());
        calamity.setDate(calamityRequest.date());
        calamity.setDamageCost(calamityRequest.damageCost());
        calamity.setCasualties(calamityRequest.casualties());
        calamity.setDescription(calamityRequest.description().trim());

        // Handle affected areas logic
        if ("BARANGAY".equals(affectedAreaType)) {
            if (calamityRequest.barangayId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Barangay is required for BARANGAY affected area type");
            }
            Barangay barangay = barangayRepository.findById(calamityRequest.barangayId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barangay not found"));
            validateBatadBarangay(barangay);
            calamity.setBarangay(barangay);

            replaceAffectedBarangays(calamity, List.of(barangay));

        } else if ("MULTI_BARANGAY".equals(affectedAreaType)) {
            if (calamityRequest.barangayIds() == null || calamityRequest.barangayIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one barangay is required");
            }
            List<Barangay> barangays = new ArrayList<>();
            for (Long bId : calamityRequest.barangayIds()) {
                Barangay b = barangayRepository.findById(bId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barangay not found: " + bId));
                validateBatadBarangay(b);
                barangays.add(b);
            }
            calamity.setBarangay(barangays.get(0));
            replaceAffectedBarangays(calamity, barangays);

        } else if ("MUNICIPALITY".equals(affectedAreaType)) {
            calamity.setBarangay(null);
            replaceAffectedBarangays(calamity, barangayRepository.findActiveBatadBarangays());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid affected area type");
        }
    }

    // helper method for mapToRequestEntity
    private void replaceAffectedBarangays(Calamity calamity, List<Barangay> barangays) {
       if(calamity.getAffectedBarangays() == null){
           calamity.setAffectedBarangays(new ArrayList<>());
       } else {
           calamity.getAffectedBarangays().clear();
       }

       calamity.getAffectedBarangays().addAll(barangays);
    }

    // --- Utility Methods ---

    @Transactional(readOnly = true)
    public List<CalamityResponse> getAllCalamityRecords() {
        return calamityRepository.findAll(Sort.by(Sort.Direction.DESC, "date"))
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getCalamitiesThisYear() {
        return calamityRepository.countByDateBetween(LocalDate.now().withDayOfYear(1), LocalDate.now());
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public void deleteCalamityRecord(long calamityId) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calamity not found"));
        calamityRepository.delete(calamity);
    }

    private void validateBatadBarangay(Barangay barangay) {
        if (!"batad".equalsIgnoreCase(barangay.getMunicipalityName())
                || !"iloilo".equalsIgnoreCase(barangay.getProvinceName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Batad, Iloilo barangays are allowed");
        }
    }

    private CalamityResponse mapToResponse(Calamity calamity) {
        List<String> affectedBarangayNames = calamity.getAffectedBarangays() != null
                ? calamity.getAffectedBarangays().stream().map(Barangay::getName).toList()
                : List.of();

        List<Long> affectedBarangayIds = calamity.getAffectedBarangays() != null
                ? calamity.getAffectedBarangays().stream().map(Barangay::getId).toList()
                : List.of();

        return new CalamityResponse(
                calamity.getId(),
                calamity.getType(),
                calamity.getEventName(),
                calamity.getStatus(),
                calamity.getAffectedAreaTypes(),
                calamity.getBarangay() != null ? calamity.getBarangay().getId() : null,
                calamity.getBarangay() != null ? calamity.getBarangay().getName() : null,
                affectedBarangayNames,
                affectedBarangayIds,
                calamity.getSeverity(),
                calamity.getDate(),
                calamity.getDamageCost(),
                calamity.getCasualties(),
                calamity.getDescription(),
                calamity.getCoordinator() != null ? calamity.getCoordinator().getId() : null,
                calamity.getCoordinator() != null ? calamity.getCoordinator().getFullName() : null
        );
    }


    // delete this later
    @Transactional(readOnly = true)
    public CalamityResponse getCalamityById(long calamityId) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calamity not found: " + calamityId));

        return mapToResponse(calamity);
    }
}