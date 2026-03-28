package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.ApprovalDecisionRequest;
import com.isufst.mdrrmosystem.request.ApprovalRequestCreateRequest;
import com.isufst.mdrrmosystem.response.ApprovalRequestResponse;
import com.isufst.mdrrmosystem.service.ApprovalRequestService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

    public ApprovalRequestController(ApprovalRequestService approvalRequestService) {
        this.approvalRequestService = approvalRequestService;
    }

    @PostMapping
    public ApprovalRequestResponse create(@Valid @RequestBody ApprovalRequestCreateRequest request) {
        return approvalRequestService.createRequest(request);
    }

    @GetMapping("/mine")
    public List<ApprovalRequestResponse> getMine() {
        return approvalRequestService.getMyRequests();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<ApprovalRequestResponse> getPending() {
        return approvalRequestService.getPendingRequests();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApprovalRequestResponse approve(@PathVariable Long id,
                                           @RequestBody ApprovalDecisionRequest request) {
        return approvalRequestService.approve(id, request);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApprovalRequestResponse reject(@PathVariable Long id,
                                          @RequestBody ApprovalDecisionRequest request) {
        return approvalRequestService.reject(id, request);
    }
}
