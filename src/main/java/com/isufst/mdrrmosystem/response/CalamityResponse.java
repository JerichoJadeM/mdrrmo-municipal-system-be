package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalamityResponse(
        long id,
        String type,
        String status,
        String barangayName,
        String severity,
        LocalDate date,
        BigDecimal damageCost,
        Integer casualties,
        String description,
        Long coordinatorId,
        String coordinatorName
) { }
