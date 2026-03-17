package com.isufst.mdrrmosystem.response;

public record EvacuationCheckResponse(
        Long centerId,
        String centerName,
        String barangayName,
        int capacity,
        int currentEvacuees,
        int availableSlots,
        String status
) {
}