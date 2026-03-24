package com.isufst.mdrrmosystem.response;

import java.util.List;

public record AlertsReadinessDomainResponse(
        String type,
        String title,
        String description,
        String status,
        int score,
        String note,
        List<AlertsMetricResponse> metrics
) {
}
