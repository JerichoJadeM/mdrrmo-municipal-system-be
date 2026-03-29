package com.isufst.mdrrmosystem.security;

import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.service.ApprovalRequestService;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Component;

@Component("operationTransitionAccessEvaluator")
public class OperationTransitionAccessEvaluator {

    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ApprovalRequestService approvalRequestService;

    public OperationTransitionAccessEvaluator(FindAuthenticatedUser findAuthenticatedUser,
                                              ApprovalRequestService approvalRequestService) {
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.approvalRequestService = approvalRequestService;
    }

    public boolean canTransition(String referenceType, Long referenceId, String mode) {
        User actor = findAuthenticatedUser.getAuthenticatedUser();
        if (actor == null) {
            return false;
        }

        boolean isElevated = actor.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                        "ROLE_MANAGER".equals(a.getAuthority())
        );

        if (isElevated) {
            return true;
        }

        return approvalRequestService.hasApprovedOperationAcknowledgement(
                String.valueOf(referenceType).trim().toUpperCase(),
                referenceId,
                mode != null ? mode.trim().toUpperCase() : null
        );
    }
}