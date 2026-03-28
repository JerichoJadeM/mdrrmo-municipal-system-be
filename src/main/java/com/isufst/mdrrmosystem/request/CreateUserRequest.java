package com.isufst.mdrrmosystem.request;

import java.util.List;

public record CreateUserRequest(
        String firstName,
        String middleName,
        String lastName,
        String email,
        String number,
        String password,
        String position,
        String office,
        String accountStatus,
        Boolean responderEligible,
        Boolean coordinatorEligible,
        List<String> authorities
) {
}
