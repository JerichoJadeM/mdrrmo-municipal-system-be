package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.response.OperationReliefStatusResponse;

public interface OperationReliefStatusService {
    OperationReliefStatusResponse getIncidentReliefStatus(Long incidentId);
    OperationReliefStatusResponse getCalamityReliefStatus(Long calamityId);
}