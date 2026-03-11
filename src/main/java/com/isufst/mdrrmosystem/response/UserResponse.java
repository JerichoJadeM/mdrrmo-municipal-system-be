package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Authority;

import java.util.List;

public record UserResponse(
        long id,
        String fullName,
        String email,
        String number,
        List<String> authorities
) {
}
