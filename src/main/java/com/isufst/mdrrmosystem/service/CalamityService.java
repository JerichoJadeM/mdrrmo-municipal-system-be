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
import com.isufst.mdrrmosystem.response.WarningItem;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
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
    private final NotificationService notificationService;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final OperationApprovalGuard operationApprovalGuard;

    public CalamityService(CalamityRepository calamityRepository,
                           BarangayRepository barangayRepository,
                           UserRepository userRepository,
                           OperationHistoryService operationHistoryService,
                           NotificationService notificationService,
                           FindAuthenticatedUser findAuthenticatedUser,
                           OperationApprovalGuard operationApprovalGuard) {
        this.calamityRepository = calamityRepository;
        this.barangayRepository = barangayRepository;
        this.userRepository = userRepository;
        this.operationHistoryService = operationHistoryService;
        this.notificationService = notificationService;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.operationApprovalGuard = operationApprovalGuard;
    }

    @Transactional
    public CalamityResponse addCalamityRecord(CalamityRequest calamityRequest) {
        Calamity calamity = new Calamity();
        calamity.setStatus("ACTIVE");

        mapRequestToEntity(calamity, calamityRequest);

        Calamity savedCalamity = calamityRepository.save(calamity);

        notifyCoordinatorIfAssigned(savedCalamity, "assigned as coordinator for calamity " + savedCalamity.getType());
        notifyAllUsersIfHighOrCritical(savedCalamity, "Calamity marked HIGH/CRITICAL: " + savedCalamity.getType());

        return mapToResponse(savedCalamity);
    }

//    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public CalamityResponse updateCalamityRecord(long calamityId, CalamityRequest calamityRequest) {
        Calamity existingCalamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        Long oldCoordinatorId = existingCalamity.getCoordinator() != null ? existingCalamity.getCoordinator().getId() : null;
        String oldSeverity = existingCalamity.getSeverity();

        mapRequestToEntity(existingCalamity, calamityRequest);

        Calamity updatedCalamity = calamityRepository.save(existingCalamity);

        Long newCoordinatorId = updatedCalamity.getCoordinator() != null ? updatedCalamity.getCoordinator().getId() : null;
        if (newCoordinatorId != null && !newCoordinatorId.equals(oldCoordinatorId)) {
            notifyCoordinatorIfAssigned(updatedCalamity, "assigned as coordinator for calamity " + updatedCalamity.getType());
        }

        if (isSeverityEscalatedToHighOrCritical(oldSeverity, updatedCalamity.getSeverity())) {
            notifyAllUsersIfHighOrCritical(updatedCalamity, "Calamity escalated to HIGH/CRITICAL: " + updatedCalamity.getType());
        }

        return mapToResponse(updatedCalamity);
    }


    @PreAuthorize("@operationTransitionAccessEvaluator.canTransition('CALAMITY', #calamityId, 'MONITOR_REVIEW')")
    @Transactional
    public CalamityResponse markCalamityMonitoring(long calamityId, CalamityTransitionRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        if (!"ACTIVE".equalsIgnoreCase(calamity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only ACTIVE calamities can be moved to MONITORING");
        }

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        operationApprovalGuard.validateOrThrowForCalamityTransition(
                actor,
                calamity,
                "MONITORING",
                buildCalamityWarnings(calamity)
        );

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

    @PreAuthorize("@operationTransitionAccessEvaluator.canTransition('CALAMITY', #calamityId, 'RESOLVE_REVIEW')")
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

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        operationApprovalGuard.validateOrThrowForCalamityTransition(
                actor,
                calamity,
                "RESOLVED",
                buildCalamityWarnings(calamity)
        );

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

    @PreAuthorize("@operationTransitionAccessEvaluator.canTransition('CALAMITY', #calamityId, 'END_REVIEW')")
    @Transactional
    public CalamityResponse markCalamityEnded(long calamityId, CalamityTransitionRequest request) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calamity Id not found: " + calamityId));

        if (!"RESOLVED".equalsIgnoreCase(calamity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only RESOLVED calamities can be moved to ENDED");
        }

        User actor = findAuthenticatedUser.getAuthenticatedUser();
        operationApprovalGuard.validateOrThrowForCalamityTransition(
                actor,
                calamity,
                "ENDED",
                buildCalamityWarnings(calamity)
        );

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

    private void notifyCoordinatorIfAssigned(Calamity calamity, String message) {
        if (calamity.getCoordinator() == null) {
            return;
        }

        notificationService.notifyUser(
                calamity.getCoordinator(),
                "ASSIGNMENT",
                "Calamity Coordinator Assignment",
                message,
                "CALAMITY",
                calamity.getId()
        );
    }

    private void notifyAllUsersIfHighOrCritical(Calamity calamity, String message) {
        if (!isHighOrCritical(calamity.getSeverity())) {
            return;
        }

        notificationService.notifyAllUsers(
                "WARNING",
                "High/Critical Calamity Alert",
                message,
                "CALAMITY",
                calamity.getId()
        );
    }

    private boolean isHighOrCritical(String severity) {
        if (severity == null) return false;
        String value = severity.trim().toUpperCase();
        return "HIGH".equals(value) || "CRITICAL".equals(value);
    }

    private boolean isSeverityEscalatedToHighOrCritical(String oldSeverity, String newSeverity) {
        return !isHighOrCritical(oldSeverity) && isHighOrCritical(newSeverity);
    }

    private List<WarningItem> buildCalamityWarnings(Calamity calamity) {
        if (isHighOrCritical(calamity.getSeverity())) {
            return List.of(
                    new WarningItem(
                            "WARNING",
                            "CALAMITY_HIGH_SEVERITY",
                            "High-severity calamity transition requires acknowledgement.",
                            "Submit acknowledgement request or approve as manager/admin.",
                            true,
                            true
                    )
            );
        }
        return List.of();
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

            validateCoordinatorAssignable(coordinator);
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

        // keep your existing affected area logic below this
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

//  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
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

    private void validateCoordinatorAssignable(User coordinator) {
        if (coordinator == null) return;

        if (!"ACTIVE".equalsIgnoreCase(coordinator.getAccountStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned coordinator is not active.");
        }

        if (!Boolean.TRUE.equals(coordinator.getCoordinatorEligible())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not coordinator-eligible.");
        }

        if ("OFF_DUTY".equalsIgnoreCase(coordinator.getAssignmentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned coordinator is off duty.");
        }
    }
}