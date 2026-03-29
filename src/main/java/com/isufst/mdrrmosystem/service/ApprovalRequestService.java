package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.ApprovalDecisionRequest;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;
import com.isufst.mdrrmosystem.response.ApprovalRequestStatusResponse;

import java.util.List;

public interface ApprovalRequestService {
    ApprovalRequestResponse createRequest(ApprovalRequestCreateRequest request);
    ApprovalRequestResponse approve(Long requestId, ApprovalDecisionRequest request);
    ApprovalRequestResponse reject(Long requestId, ApprovalDecisionRequest request);
    List<ApprovalRequestResponse> getPendingRequests();
    List<ApprovalRequestResponse> getMyRequests();
    boolean hasApprovedRequest(String requestType, String referenceType, Long referenceId, Long requestedByUserId);
    ApprovalRequestStatusResponse getLatestStatus(String requestType, String referenceType, Long referenceId);
    ApprovalRequestStatusResponse getLatestStatus(String requestType, String referenceType, Long referenceId, String mode);
    boolean hasApprovedOperationAcknowledgement(String referenceType, Long referenceId, String mode);
}
