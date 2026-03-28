package com.isufst.mdrrmosystem.request;

public record ApprovalDecisionRequest(
        String decision, // APPROVE or REJECT
        String remarks
) {}
