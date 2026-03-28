package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.ApprovalDecisionRequest;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;

import java.util.List;

public interface ApprovalRequestService {
    ApprovalRequestResponse createRequest(ApprovalRequestCreateRequest request);
    ApprovalRequestResponse approve(Long requestId, ApprovalDecisionRequest request);
    ApprovalRequestResponse reject(Long requestId, ApprovalDecisionRequest request);
    List<ApprovalRequestResponse> getPendingRequests();
    List<ApprovalRequestResponse> getMyRequests();
    boolean hasApprovedRequest(String requestType, String referenceType, Long referenceId, Long requestedByUserId);
}
