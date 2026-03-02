package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ReliefDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReliefDistributionRepository extends JpaRepository<ReliefDistribution, Long> {

    List<ReliefDistribution> findByIncident_Id(Long incidentId);

    List<ReliefDistribution> findByEvacuationActivation_Id(Long evacuationId);
}
