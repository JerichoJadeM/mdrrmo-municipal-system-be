package com.isufst.mdrrmosystem.request;

public record UpdateMyProfileRequest(
        String firstName,
        String middleName,
        String lastName,
        String number,
        String position,
        String office
) {
}
