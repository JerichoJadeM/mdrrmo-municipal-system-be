package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.AdminActionLog;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.AdminActionLogRepository;
import com.isufst.mdrrmosystem.response.AdminActionLogResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {
    private final AdminActionLogRepository adminActionLogRepository;
    public AdminAuditServiceImpl(AdminActionLogRepository adminActionLogRepository) { this.adminActionLogRepository = adminActionLogRepository; }

    @Override
    public void log(User actor, User targetUser, String actionType, String description) {
        AdminActionLog log = new AdminActionLog();
        log.setActor(actor);
        log.setTargetUser(targetUser);
        log.setActionType(actionType);
        log.setDescription(description);
        adminActionLogRepository.save(log);
    }

    @Override
    public List<AdminActionLogResponse> getRecentLogs() {
        return adminActionLogRepository.findTop20ByOrderByCreatedAtDesc().stream().map(this::map).toList();
    }

    private AdminActionLogResponse map(AdminActionLog log) {
        return new AdminActionLogResponse(log.getId(),
                log.getActor() != null ? log.getActor().getId() : null,
                log.getActor() != null ? log.getActor().getFullName() : null,
                log.getTargetUser() != null ? log.getTargetUser().getId() : null,
                log.getTargetUser() != null ? log.getTargetUser().getFullName() : null,
                log.getActionType(), log.getDescription(), log.getCreatedAt());
    }
}