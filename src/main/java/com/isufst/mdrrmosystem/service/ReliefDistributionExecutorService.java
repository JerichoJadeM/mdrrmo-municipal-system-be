package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.request.ReliefDistributionRequest;

public interface ReliefDistributionExecutorService {
    void executeIncidentDistribution(Long incidentId, ReliefDistributionRequest request, User actor);
    void executeCalamityDistribution(Long calamityId, ReliefDistributionRequest request, User actor);
}
