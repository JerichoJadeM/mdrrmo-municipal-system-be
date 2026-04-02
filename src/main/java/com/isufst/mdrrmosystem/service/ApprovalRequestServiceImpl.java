package com.isufst.mdrrmosystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isufst.mdrrmosystem.entity.ApprovalRequest;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.ApprovalRequestRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.*;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;
import com.isufst.mdrrmosystem.response.ApprovalRequestStatusResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalRequestServiceImpl implements ApprovalRequestService {
    private final ApprovalRequestRepository approvalRequestRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final InventoryProcurementExecutorService inventoryProcurementExecutorService;
    private final ReliefDistributionExecutorService reliefDistributionExecutorService;
    private final ReliefPackDistributionExecutorService reliefPackDistributionExecutorService;

    public ApprovalRequestServiceImpl(ApprovalRequestRepository approvalRequestRepository,
                                      FindAuthenticatedUser findAuthenticatedUser,
                                      NotificationService notificationService,
                                      AdminAuditService adminAuditService,
                                      UserRepository userRepository,
                                      ObjectMapper objectMapper,
                                      InventoryProcurementExecutorService inventoryProcurementExecutorService,
                                      ReliefDistributionExecutorService reliefDistributionExecutorService,
                                      ReliefPackDistributionExecutorService reliefPackDistributionExecutorService
                                      ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.notificationService = notificationService;
        this.adminAuditService = adminAuditService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.inventoryProcurementExecutorService = inventoryProcurementExecutorService;
        this.reliefDistributionExecutorService = reliefDistributionExecutorService;
        this.reliefPackDistributionExecutorService = reliefPackDistributionExecutorService;
    }

    @Override
    @Transactional
    public ApprovalRequestResponse createRequest(ApprovalRequestCreateRequest request) {
        User authUser = findAuthenticatedUser.getAuthenticatedUser();

        if (authUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user could not be resolved.");
        }

        User requestedBy = null;

        if (authUser.getId() != null) {
            requestedBy = userRepository.findById(authUser.getId()).orElse(null);
        }

        if (requestedBy == null && authUser.getEmail() != null && !authUser.getEmail().isBlank()) {
            requestedBy = userRepository.findByEmail(authUser.getEmail()).orElse(null);
        }

        if (requestedBy == null && authUser.getUsername() != null && !authUser.getUsername().isBlank()) {
            requestedBy = userRepository.findByUsername(authUser.getUsername()).orElse(null);
        }

        if (requestedBy == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authenticated user is not linked to a valid users record."
            );
        }

        String requestType = request.requestType().trim().toUpperCase();
        String referenceType = request.referenceType().trim().toUpperCase();

        if ("OPERATION_ACKNOWLEDGEMENT".equals(requestType)) {
            String mode = extractModeFromPayload(request.payloadJson());

            boolean pendingExists = approvalRequestRepository.existsPendingByRequestTypeReferenceAndMode(
                    requestType,
                    referenceType,
                    request.referenceId(),
                    mode
            );

            if (pendingExists) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A pending acknowledgement request already exists for this operation step."
                );
            }
        }

        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setRequestType(requestType);
        approvalRequest.setStatus("PENDING");
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setTitle(request.title().trim());
        approvalRequest.setDescription(request.description().trim());
        approvalRequest.setReferenceType(referenceType);
        approvalRequest.setReferenceId(request.referenceId());
        approvalRequest.setPayloadJson(request.payloadJson());

        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);

        notificationService.notifyAdminsAndManagers(
                "REQUEST",
                "Approval Request Submitted",
                request.title(),
                "APPROVAL_REQUEST",
                saved.getId()
        );

        return map(saved);
    }

    @Override
    @Transactional
    public ApprovalRequestResponse approve(Long requestId, ApprovalDecisionRequest request) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        if (!"PENDING".equalsIgnoreCase(approvalRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be approved");
        }

        User reviewer = findAuthenticatedUser.getAuthenticatedUser();
        Long reviewerId = reviewer != null ? reviewer.getId() : null;
        if (reviewerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reviewer could not be resolved.");
        }

        User resolvedReviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reviewer is not linked to a valid user record."));

        executeApprovalAction(approvalRequest, resolvedReviewer);

        approvalRequest.setStatus("APPROVED");
        approvalRequest.setReviewedBy(resolvedReviewer);
        approvalRequest.setReviewedAt(LocalDateTime.now());
        approvalRequest.setReviewRemarks(request != null ? request.remarks() : null);

        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);

        notificationService.notifyUser(
                approvalRequest.getRequestedBy(),
                "REQUEST",
                buildApprovalCompletedTitle(approvalRequest),
                buildApprovalCompletedMessage(approvalRequest, resolvedReviewer),
                "APPROVAL_REQUEST",
                saved.getId()
        );

        adminAuditService.log(
                resolvedReviewer,
                approvalRequest.getRequestedBy(),
                "APPROVAL_APPROVED",
                "Approval request approved: " + approvalRequest.getTitle()
        );

        return map(saved);
    }

    @Override
    @Transactional
    public ApprovalRequestResponse reject(Long requestId, ApprovalDecisionRequest request) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        if (!"PENDING".equalsIgnoreCase(approvalRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be rejected");
        }

        User reviewer = findAuthenticatedUser.getAuthenticatedUser();
        Long reviewerId = reviewer != null ? reviewer.getId() : null;
        if (reviewerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reviewer could not be resolved.");
        }

        User resolvedReviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reviewer is not linked to a valid user record."));

        String remarks = request != null ? request.remarks() : null;

        approvalRequest.setStatus("REJECTED");
        approvalRequest.setReviewedBy(resolvedReviewer);
        approvalRequest.setReviewedAt(LocalDateTime.now());
        approvalRequest.setReviewRemarks(remarks);

        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);

        notificationService.notifyUser(
                approvalRequest.getRequestedBy(),
                "REQUEST",
                buildRejectedTitle(approvalRequest),
                buildRejectedMessage(approvalRequest, resolvedReviewer, remarks),
                "APPROVAL_REQUEST",
                saved.getId()
        );

        adminAuditService.log(
                resolvedReviewer,
                approvalRequest.getRequestedBy(),
                "APPROVAL_REJECTED",
                "Approval request rejected: " + approvalRequest.getTitle()
        );

        return map(saved);
    }

    @Override public List<ApprovalRequestResponse> getPendingRequests() {
        return approvalRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING").stream().map(this::map).toList();
    }

    @Override public List<ApprovalRequestResponse> getMyRequests() {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        return approvalRequestRepository.findByRequestedBy_IdOrderByCreatedAtDesc(currentUser.getId()).stream().map(this::map).toList();
    }

    @Override public boolean hasApprovedRequest(String requestType, String referenceType, Long referenceId, Long requestedByUserId) {
        return approvalRequestRepository.findTopByRequestTypeAndReferenceTypeAndReferenceIdAndRequestedBy_IdAndStatusOrderByCreatedAtDesc(
                requestType.trim().toUpperCase(), referenceType.trim().toUpperCase(), referenceId, requestedByUserId, "APPROVED").isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequestStatusResponse getLatestStatus(String requestType, String referenceType, Long referenceId) {
        String normalizedRequestType = String.valueOf(requestType).trim().toUpperCase();
        String normalizedReferenceType = String.valueOf(referenceType).trim().toUpperCase();

        ApprovalRequest approvalRequest = approvalRequestRepository
                .findTopByRequestTypeAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
                        normalizedRequestType,
                        normalizedReferenceType,
                        referenceId
                )
                .orElse(null);

        if (approvalRequest == null) {
            return new ApprovalRequestStatusResponse(
                    null,
                    "NONE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new ApprovalRequestStatusResponse(
                approvalRequest.getId(),
                approvalRequest.getStatus(),
                approvalRequest.getRequestedBy() != null ? approvalRequest.getRequestedBy().getId() : null,
                approvalRequest.getRequestedBy() != null ? approvalRequest.getRequestedBy().getFullName() : null,
                approvalRequest.getReviewedBy() != null ? approvalRequest.getReviewedBy().getId() : null,
                approvalRequest.getReviewedBy() != null ? approvalRequest.getReviewedBy().getFullName() : null,
                approvalRequest.getReviewedAt(),
                approvalRequest.getReviewRemarks()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequestStatusResponse getLatestStatus(String requestType, String referenceType, Long referenceId, String mode) {
        String normalizedRequestType = String.valueOf(requestType).trim().toUpperCase();
        String normalizedReferenceType = String.valueOf(referenceType).trim().toUpperCase();
        String normalizedMode = mode != null && !mode.isBlank() ? mode.trim().toUpperCase() : null;

        ApprovalRequest approvalRequest = approvalRequestRepository
                .findLatestByRequestTypeReferenceAndMode(
                        normalizedRequestType,
                        normalizedReferenceType,
                        referenceId,
                        normalizedMode
                )
                .stream()
                .findFirst()
                .orElse(null);

        if (approvalRequest == null) {
            return new ApprovalRequestStatusResponse(
                    null,
                    "NONE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new ApprovalRequestStatusResponse(
                approvalRequest.getId(),
                approvalRequest.getStatus(),
                approvalRequest.getRequestedBy() != null ? approvalRequest.getRequestedBy().getId() : null,
                approvalRequest.getRequestedBy() != null ? approvalRequest.getRequestedBy().getFullName() : null,
                approvalRequest.getReviewedBy() != null ? approvalRequest.getReviewedBy().getId() : null,
                approvalRequest.getReviewedBy() != null ? approvalRequest.getReviewedBy().getFullName() : null,
                approvalRequest.getReviewedAt(),
                approvalRequest.getReviewRemarks()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedOperationAcknowledgement(String referenceType, Long referenceId, String mode) {
        String normalizedReferenceType = String.valueOf(referenceType).trim().toUpperCase();
        String normalizedMode = mode != null && !mode.isBlank() ? mode.trim().toUpperCase() : null;

        return approvalRequestRepository.existsApprovedOperationAcknowledgement(
                normalizedReferenceType,
                referenceId,
                normalizedMode
        );
    }

    private String extractModeFromPayload(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) return null;
            JsonNode node = objectMapper.readTree(payloadJson);
            JsonNode modeNode = node.get("mode");
            return modeNode != null && !modeNode.isNull() ? modeNode.asText().trim().toUpperCase() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void executeApprovalAction(ApprovalRequest approvalRequest, User reviewer) {
        String requestType = String.valueOf(approvalRequest.getRequestType()).trim().toUpperCase();

        switch (requestType) {
            case "NEW_INVENTORY_PROCUREMENT_REQUEST" -> executeNewInventoryProcurementApproval(approvalRequest, reviewer);
            case "PROCUREMENT_REQUEST" -> executeProcurementApproval(approvalRequest, reviewer);
            case "RELIEF_DISTRIBUTION_REQUEST" -> executeReliefDistributionApproval(approvalRequest, reviewer);
            case "RELIEF_PACK_DISTRIBUTION_REQUEST" -> executeReliefPackDistributionApproval(approvalRequest, reviewer);
            case "OPERATION_ACKNOWLEDGEMENT" -> executeOperationAcknowledgementApproval(approvalRequest, reviewer);
            default -> {
                // status-only approval for unknown/simple request types
            }
        }
    }

    private void executeNewInventoryProcurementApproval(ApprovalRequest approvalRequest, User reviewer) {
        try {
            InventoryCreateProcurementRequest request = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    InventoryCreateProcurementRequest.class
            );

            Inventory createdInventory =
                    inventoryProcurementExecutorService.executeNewInventoryProcurement(request, reviewer);

            notificationService.notifyAllUsers(
                    "INVENTORY",
                    "New Inventory Item Procured",
                    "Approved new inventory procurement has been completed for " + createdInventory.getName() + ".",
                    "INVENTORY",
                    createdInventory.getId()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to execute new inventory procurement approval: " + ex.getMessage()
            );
        }
    }

    private void executeOperationAcknowledgementApproval(ApprovalRequest approvalRequest, User reviewer) {
        notificationService.notifyUser(
                approvalRequest.getRequestedBy(),
                "REQUEST",
                "Operation Acknowledgement Approved",
                "Operation acknowledgement was approved by " + reviewer.getFullName() + ". You may now continue the transition.",
                "APPROVAL_REQUEST",
                approvalRequest.getId()
        );
    }

    private void executeProcurementApproval(ApprovalRequest approvalRequest, User reviewer) {
        try {
            ProcurementPayload payload = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    ProcurementPayload.class
            );

            InventoryProcurementRequest request = new InventoryProcurementRequest(
                    payload.categoryId(),
                    payload.quantityAdded(),
                    payload.totalCost(),
                    payload.expenseDate() != null && !payload.expenseDate().isBlank()
                            ? LocalDate.parse(payload.expenseDate())
                            : null,
                    payload.description(),
                    payload.incidentId(),
                    payload.calamityId()
            );

            inventoryProcurementExecutorService.executeProcurement(payload.inventoryId(), request, reviewer);

            notificationService.notifyAllUsers(
                    "INVENTORY",
                    "Procurement Completed",
                    "Approved procurement completed for inventory item #" + payload.inventoryId(),
                    "INVENTORY",
                    payload.inventoryId()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to execute procurement approval: " + ex.getMessage()
            );
        }
    }

    private record ProcurementPayload(
            Long inventoryId,
            Integer quantityAdded,
            Double totalCost,
            Long categoryId,
            Long incidentId,
            Long calamityId,
            String expenseDate,
            String description
    ) {}

    private void executeReliefDistributionApproval(ApprovalRequest approvalRequest, User reviewer) {
        try {
            ReliefDistributionPayload payload = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    ReliefDistributionPayload.class
            );

            ReliefDistributionRequest request = new ReliefDistributionRequest(
                    payload.inventoryId(),
                    payload.quantity(),
                    payload.evacuationActivationId(),
                    payload.calamityId()
            );

            if (payload.incidentId() != null) {
                reliefDistributionExecutorService.executeIncidentDistribution(payload.incidentId(), request, reviewer);
            } else if (payload.calamityId() != null) {
                reliefDistributionExecutorService.executeCalamityDistribution(payload.calamityId(), request, reviewer);
            } else {
                throw new RuntimeException("Missing incidentId/calamityId in relief distribution payload.");
            }

            notificationService.notifyAllUsers(
                    "INVENTORY",
                    "Relief Distribution Completed",
                    "Approved relief distribution has been completed.",
                    "APPROVAL_REQUEST",
                    approvalRequest.getId()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to execute relief distribution approval: " + ex.getMessage()
            );
        }
    }

    private void executeReliefPackDistributionApproval(ApprovalRequest approvalRequest, User reviewer) {
        try {
            ReliefPackPayload payload = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    ReliefPackPayload.class
            );

            if (payload.incidentId() != null) {
                reliefPackDistributionExecutorService.executeIncidentPackDistribution(
                        payload.templateId(),
                        payload.incidentId(),
                        payload.packCount(),
                        payload.evacuationActivationId(),
                        reviewer
                );
            } else if (payload.calamityId() != null) {
                reliefPackDistributionExecutorService.executeCalamityPackDistribution(
                        payload.templateId(),
                        payload.calamityId(),
                        payload.packCount(),
                        payload.evacuationActivationId(),
                        reviewer
                );
            } else {
                throw new RuntimeException("Missing incidentId/calamityId in relief pack payload.");
            }

            notificationService.notifyAllUsers(
                    "INVENTORY",
                    "Relief Pack Distribution Completed",
                    "Approved relief pack distribution has been completed.",
                    "APPROVAL_REQUEST",
                    approvalRequest.getId()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to execute relief pack approval: " + ex.getMessage()
            );
        }
    }

    private ApprovalRequestResponse decide(Long requestId, ApprovalDecisionRequest request, String newStatus) {
        User reviewer = findAuthenticatedUser.getAuthenticatedUser();
        ApprovalRequest approvalRequest = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));

        if (!"PENDING".equalsIgnoreCase(approvalRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be decided");
        }

        approvalRequest.setStatus(newStatus);
        approvalRequest.setReviewedBy(reviewer);
        approvalRequest.setReviewedAt(LocalDateTime.now());
        approvalRequest.setReviewRemarks(request.remarks());
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);

        notificationService.notifyUser(saved.getRequestedBy(), "REQUEST_DECISION", "Approval Request " + newStatus, saved.getTitle(), "APPROVAL_REQUEST", saved.getId());
        adminAuditService.log(reviewer, saved.getRequestedBy(), "APPROVAL_DECISION", reviewer.getFullName() + " marked request #" + saved.getId() + " as " + newStatus);
        return map(saved);
    }

    private String buildApprovalCompletedTitle(ApprovalRequest approvalRequest) {
        String type = String.valueOf(approvalRequest.getRequestType()).trim().toUpperCase();

        return switch (type) {
            case "PROCUREMENT_REQUEST" -> "Procurement Approved";
            case "RELIEF_DISTRIBUTION_REQUEST" -> "Relief Distribution Approved";
            case "RELIEF_PACK_DISTRIBUTION_REQUEST" -> "Relief Pack Distribution Approved";
            default -> "Approval Request Approved";
        };
    }

    private String buildApprovalCompletedMessage(ApprovalRequest approvalRequest, User reviewer) {
        String reviewerName = reviewer != null ? reviewer.getFullName() : "a manager/admin";
        String type = String.valueOf(approvalRequest.getRequestType()).trim().toUpperCase();

        return switch (type) {
            case "NEW_INVENTORY_PROCUREMENT_REQUEST" ->
                    "New inventory procurement was approved by " + reviewerName + " and has been completed.";
            case "PROCUREMENT_REQUEST" ->
                    "Procurement was approved by " + reviewerName + " and has been completed.";
            case "RELIEF_DISTRIBUTION_REQUEST" ->
                    "Relief distribution was approved by " + reviewerName + " and has been completed.";
            case "RELIEF_PACK_DISTRIBUTION_REQUEST" ->
                    "Relief pack distribution was approved by " + reviewerName + " and has been completed.";
            default ->
                    "Your approval request was approved by " + reviewerName + ".";
        };
    }

    private String buildRejectedTitle(ApprovalRequest approvalRequest) {
        String type = String.valueOf(approvalRequest.getRequestType()).trim().toUpperCase();

        return switch (type) {
            case "PROCUREMENT_REQUEST" -> "Procurement Rejected";
            case "RELIEF_DISTRIBUTION_REQUEST" -> "Relief Distribution Rejected";
            case "RELIEF_PACK_DISTRIBUTION_REQUEST" -> "Relief Pack Distribution Rejected";
            default -> "Approval Request Rejected";
        };
    }

    private String buildRejectedMessage(ApprovalRequest approvalRequest, User reviewer, String remarks) {
        String reviewerName = reviewer != null ? reviewer.getFullName() : "a manager/admin";
        String type = String.valueOf(approvalRequest.getRequestType()).trim().toUpperCase();

        String base = switch (type) {
            case "NEW_INVENTORY_PROCUREMENT_REQUEST" ->
                    "New inventory procurement request was rejected by " + reviewerName + ".";
            case "PROCUREMENT_REQUEST" ->
                    "Procurement request was rejected by " + reviewerName + ".";
            case "RELIEF_DISTRIBUTION_REQUEST" ->
                    "Relief distribution request was rejected by " + reviewerName + ".";
            case "RELIEF_PACK_DISTRIBUTION_REQUEST" ->
                    "Relief pack distribution request was rejected by " + reviewerName + ".";
            default ->
                    "Your approval request was rejected by " + reviewerName + ".";
        };

        if (remarks != null && !remarks.isBlank()) {
            return base + " Remarks: " + remarks;
        }

        return base;
    }



    private ApprovalRequestResponse map(ApprovalRequest item) {
        return new ApprovalRequestResponse(item.getId(), item.getRequestType(), item.getStatus(),
                item.getRequestedBy() != null ? item.getRequestedBy().getId() : null,
                item.getRequestedBy() != null ? item.getRequestedBy().getFullName() : null,
                item.getReviewedBy() != null ? item.getReviewedBy().getId() : null,
                item.getReviewedBy() != null ? item.getReviewedBy().getFullName() : null,
                item.getTitle(), item.getDescription(), item.getReferenceType(), item.getReferenceId(),
                item.getPayloadJson(), item.getReviewRemarks(), item.getCreatedAt(), item.getReviewedAt());
    }

    private record ReliefDistributionPayload(
            Long incidentId,
            Long calamityId,
            Long inventoryId,
            Integer quantity,
            Long evacuationActivationId
    ) {}

    private record ReliefPackPayload(
            Long templateId,
            Long incidentId,
            Long calamityId,
            Integer packCount,
            Long evacuationActivationId
    ) {}
}
