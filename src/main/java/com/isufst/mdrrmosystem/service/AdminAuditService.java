package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.response.AdminActionLogResponse;

import java.util.List;

public interface AdminAuditService {
    void log(User actor, User targetUser, String actionType, String description);
    List<AdminActionLogResponse> getRecentLogs();
}
