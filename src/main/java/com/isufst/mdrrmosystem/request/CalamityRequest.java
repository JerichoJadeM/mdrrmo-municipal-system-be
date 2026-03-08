package com.isufst.mdrrmosystem.request;

import com.isufst.mdrrmosystem.entity.Barangay;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalamityRequest (

        @NotBlank(message = "Type is required")
        String type,

        @NotNull(message = "Barangay is required")
        Long barangayId,

        Long coordinatorId,

        @NotBlank(message = "Severity is required")
        String severity,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Damage cost is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Damage cost must be 0 or greater")
        BigDecimal damageCost,

        @NotNull(message = "Casualties is required")
        @Min(value = 0, message = "Casualties must be 0 or greater")
        Integer casualties,

        @NotBlank(message = "Description is required")
        String description
){ }
