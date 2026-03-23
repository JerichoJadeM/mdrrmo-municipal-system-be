package com.isufst.mdrrmosystem.response;

import java.time.LocalDate;

public record CalamityReportItemResponse(
        Long id,
        String calamityType,
        String status,
        String affectedArea,
        String location,
        LocalDate date
) {
}
