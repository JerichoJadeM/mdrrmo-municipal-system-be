package com.isufst.mdrrmosystem.response;

import java.math.BigDecimal;

public record EvacuationCenterResourceResponse(
        Long id,
        String name,
        Long barangayId,
        String barangayName,
        Integer capacity,
        Integer currentEvacuees,
        Integer availableSlots,
        Integer occupancyRate,
        String capacityStatus,
        String locationDetails,
        BigDecimal latitude,
        BigDecimal longitude,
        String status
) {
}
