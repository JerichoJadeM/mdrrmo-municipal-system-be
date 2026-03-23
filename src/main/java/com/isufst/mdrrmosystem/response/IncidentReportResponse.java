package com.isufst.mdrrmosystem.response;

import java.util.List;

public record IncidentReportResponse(
        long totalIncidents,
        long activeIncidents,
        long resolvedIncidents,
        List<CountByLabelResponse> byType,
        List<CountByLabelResponse> byBarangay,
        List<IncidentReportItemResponse> incidents
) {
}
