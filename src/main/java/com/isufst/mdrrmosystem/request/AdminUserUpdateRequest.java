package com.isufst.mdrrmosystem.request;

public record AdminUserUpdateRequest(
        String position,
        String office,
        String accountStatus,
        Boolean responderEligible,
        Boolean coordinatorEligible,
        String profileImageUrl
) {}
