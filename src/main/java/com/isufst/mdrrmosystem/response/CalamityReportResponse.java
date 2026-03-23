package com.isufst.mdrrmosystem.response;

import java.util.List;

public record CalamityReportResponse(
        long totalCalamities,
        long activeCalamities,
        long monitoringCalamities,
        long resolvedCalamities,
        long endedCalamities,
        List<CountByLabelResponse> byType,
        List<CountByLabelResponse> byBarangay,
        List<CalamityReportItemResponse> calamities
) {
}
