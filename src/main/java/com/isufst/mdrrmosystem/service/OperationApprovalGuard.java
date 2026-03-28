package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.response.WarningItem;

import java.util.List;

public interface OperationApprovalGuard {
    void validateOrThrowForIncidentTransition(User actor, Incident incident, String transitionType, List<WarningItem> warnings);
    void validateOrThrowForCalamityTransition(User actor, Calamity calamity, String transitionType, List<WarningItem> warnings);
}
