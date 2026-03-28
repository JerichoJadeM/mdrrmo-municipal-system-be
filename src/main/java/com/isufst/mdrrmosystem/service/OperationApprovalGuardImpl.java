package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.response.WarningItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OperationApprovalGuardImpl implements OperationApprovalGuard {
    private final ApprovalRequestService approvalRequestService;
    public OperationApprovalGuardImpl(ApprovalRequestService approvalRequestService) { this.approvalRequestService = approvalRequestService; }

    @Override
    public void validateOrThrowForIncidentTransition(User actor, Incident incident, String transitionType, List<WarningItem> warnings) {
        validate("INCIDENT", incident.getId(), actor, transitionType, warnings);
    }

    @Override
    public void validateOrThrowForCalamityTransition(User actor, Calamity calamity, String transitionType, List<WarningItem> warnings) {
        validate("CALAMITY", calamity.getId(), actor, transitionType, warnings);
    }

    private void validate(String referenceType, Long referenceId, User actor, String transitionType, List<WarningItem> warnings) {
        boolean isAdminOrManager = actor.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));
        if (isAdminOrManager || warnings == null || warnings.isEmpty()) return;
        boolean requiresApproval = warnings.stream().anyMatch(item ->
                item.requiresManagerApproval() || "CRITICAL".equalsIgnoreCase(item.level()) || "WARNING".equalsIgnoreCase(item.level()));
        if (!requiresApproval) return;
        boolean approved = approvalRequestService.hasApprovedRequest("OPERATION_ACKNOWLEDGEMENT", referenceType, referenceId, actor.getId());
        if (!approved) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acknowledgement approval required before " + transitionType + " transition.");
        }
    }
}