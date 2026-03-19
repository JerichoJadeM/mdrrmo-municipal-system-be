package com.isufst.mdrrmosystem.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateEvacueesRequest(
        @NotNull(message = "Current evacuees is required")
        @Min(value = 0, message = "Current evacuees must be 0 or greater")
        Integer currentEvacuees
) {
}
