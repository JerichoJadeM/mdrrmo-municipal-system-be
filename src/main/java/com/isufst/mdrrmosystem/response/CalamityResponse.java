package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalamityResponse(
        long id,
        String type,
        String eventName,
        String status,
        String affectedAreaType,
        Long primaryBarangayId,
        String primaryBarangayName,
        List<String> affectedBarangayNames,
        List<Long> affectedBarangayIds,
        String severity,
        LocalDate date,
        BigDecimal damageCost,
        Integer casualties,
        String description,
        Long coordinatorId,
        String coordinatorName
) { }
