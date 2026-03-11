package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ReliefDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReliefDistributionRepository extends JpaRepository<ReliefDistribution, Long> {

    List<ReliefDistribution> findByIncident_Id(Long incidentId);

    List<ReliefDistribution> findByEvacuationActivation_Id(Long evacuationId);

    @Modifying
    @Query("""
        DELETE FROM ReliefDistribution rd
        WHERE rd.evacuationActivation.incident.id = :incidentId
    """)
    void deleteByIncidentId(@Param("incidentId") Long incidentId);
}
