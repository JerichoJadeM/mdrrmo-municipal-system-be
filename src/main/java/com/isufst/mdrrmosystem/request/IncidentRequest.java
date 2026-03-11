package com.isufst.mdrrmosystem.request;

import com.isufst.mdrrmosystem.entity.Barangay;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
        @NotBlank(message = "Type is required")
        String type,

        @NotNull(message = "Barangay is required")
        Long barangayId,

        @NotBlank(message = "Severity is required")
        String severity,

        @NotBlank(message = "Description is required")
        String description,

        Long assignedResponderId
) {
}
