package com.isufst.mdrrmosystem.response;

import java.util.List;

public record AlertsWarningsOverviewResponse(
        String lastUpdated,
        AlertsWarningsSummaryResponse summary,
        List<AlertsReadinessDomainResponse> readinessDomains,
        List<ActiveAlertResponse> activeAlerts,
        List<PriorityActionResponse> priorityActions,
        List<WarningHistoryResponse> recentHistory,
        List<ReadinessNoteResponse> readinessNotes
) {
}
