package com.isufst.mdrrmosystem.response;

import java.util.List;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        String number,
        String profileImageUrl,
        String position,
        String office,
        String accountStatus,
        String assignmentStatus,
        Boolean responderEligible,
        Boolean coordinatorEligible,
        List<String> authorities
) {}
