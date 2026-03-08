package com.isufst.mdrrmosystem.response;

public record AssignableUserResponse(
        long id,
        String fullName,
        String assignmentStatus
) {
}
