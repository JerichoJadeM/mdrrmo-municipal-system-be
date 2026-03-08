package com.isufst.mdrrmosystem.response;

public record ResponderResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName
) {
}
