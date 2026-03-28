package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.ApprovalRequest;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.ApprovalRequestRepository;
import com.isufst.mdrrmosystem.request.ApprovalDecisionRequest;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalRequestServiceImpl implements ApprovalRequestService {
    private final ApprovalRequestRepository approvalRequestRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;

    public ApprovalRequestServiceImpl(ApprovalRequestRepository approvalRequestRepository, FindAuthenticatedUser findAuthenticatedUser,
                                      NotificationService notificationService, AdminAuditService adminAuditService) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.notificationService = notificationService;
        this.adminAuditService = adminAuditService;
    }

    @Override
    @Transactional
    public ApprovalRequestResponse createRequest(ApprovalRequestCreateRequest request) {
        User requestedBy = findAuthenticatedUser.getAuthenticatedUser();
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setRequestType(request.requestType().trim().toUpperCase());
        approvalRequest.setStatus("PENDING");
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setTitle(request.title().trim());
        approvalRequest.setDescription(request.description().trim());
        approvalRequest.setReferenceType(request.referenceType().trim().toUpperCase());
        approvalRequest.setReferenceId(request.referenceId());
        approvalRequest.setPayloadJson(request.payloadJson());
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);

        notificationService.notifyAdminsAndManagers("REQUEST", "Approval Request Submitted", request.title(), "APPROVAL_REQUEST", saved.getId());
        return map(saved);
    }

    @Override @Transactional
    public ApprovalRequestResponse approve(Long requestId, ApprovalDecisionRequest request) { return decide(requestId, request, "APPROVED"); }

    @Override @Transactional
    public ApprovalRequestResponse reject(Long requestId, ApprovalDecisionRequest request) { return decide(requestId, request, "REJECTED"); }

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

    private ApprovalRequestResponse map(ApprovalRequest item) {
        return new ApprovalRequestResponse(item.getId(), item.getRequestType(), item.getStatus(),
                item.getRequestedBy() != null ? item.getRequestedBy().getId() : null,
                item.getRequestedBy() != null ? item.getRequestedBy().getFullName() : null,
                item.getReviewedBy() != null ? item.getReviewedBy().getId() : null,
                item.getReviewedBy() != null ? item.getReviewedBy().getFullName() : null,
                item.getTitle(), item.getDescription(), item.getReferenceType(), item.getReferenceId(),
                item.getPayloadJson(), item.getReviewRemarks(), item.getCreatedAt(), item.getReviewedAt());
    }
}
