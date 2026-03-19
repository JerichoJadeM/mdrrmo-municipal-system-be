package com.isufst.mdrrmosystem.request;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.math.BigDecimal;

public record EvacuationCenterRequest(
        String name,
        Long barangayId,
        int capacity,
        String locationDetails,
        BigDecimal latitude,
        BigDecimal longitude,
        String status
) { }
