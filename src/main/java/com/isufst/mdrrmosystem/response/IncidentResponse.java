package com.isufst.mdrrmosystem.response;

import java.time.LocalDateTime;

public class IncidentResponse {

    public Long id;
    public String type;
    public String barangay;
    public String severity;
    public String status;
    public LocalDateTime reportedAt;
    public String description;

    public IncidentResponse(Long id, String type, String barangay,
                            String severity, String status,
                            LocalDateTime reportedAt,
                            String description) {
        this.id = id;
        this.type = type;
        this.barangay = barangay;
        this.severity = severity;
        this.status = status;
        this.reportedAt = reportedAt;
        this.description = description;
    }
}
