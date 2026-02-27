package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public record EvacuationActivationResponse(
        Long id,
        String centerName,
        String barangay,
        int capacity,
        int currentEvacuees,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
}
