package com.isufst.mdrrmosystem.request;

import jakarta.validation.constraints.Size;

public record UpdateProfilePhotoRequest(
        @Size(max = 5000000, message = "Profile image URL is too long")
        String profileImageUrl
) {
}
