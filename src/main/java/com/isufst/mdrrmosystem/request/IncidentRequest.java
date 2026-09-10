package com.isufst.mdrrmosystem.request;

import com.isufst.mdrrmosystem.entity.Barangay;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IncidentRequest(
        @NotBlank(message = "Type is required")
        String type,

        @NotNull(message = "Barangay is required")
        Long barangayId,

        @NotBlank(message = "Severity is required")
        String severity,

        @NotBlank(message = "Description is required")
        String description,

        List<Long> assignedResponderIds,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude
) {
}
