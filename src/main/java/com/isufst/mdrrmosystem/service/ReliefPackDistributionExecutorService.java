package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.User;

public interface ReliefPackDistributionExecutorService {
    void executeIncidentPackDistribution(Long templateId, Long incidentId, Integer packCount, Long evacuationActivationId, User actor);
    void executeCalamityPackDistribution(Long templateId, Long calamityId, Integer packCount, Long evacuationActivationId, User actor);
}
