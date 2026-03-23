package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ReliefDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReliefDistributionRepository extends JpaRepository<ReliefDistribution, Long> {

    List<ReliefDistribution> findByIncident_Id(Long incidentId);
    List<ReliefDistribution> findByCalamity_Id(Long calamityId);

    List<ReliefDistribution> findByEvacuationActivation_Id(Long evacuationId);

    @Modifying
    @Query("""
        DELETE FROM ReliefDistribution rd
        WHERE rd.evacuationActivation.incident.id = :incidentId
    """)
    void deleteByIncidentId(@Param("incidentId") Long incidentId);

    @Query("""
        SELECT r
        FROM ReliefDistribution r
        WHERE (:fromDate IS NULL OR r.distributedAt >= :fromDate)
          AND (:toDate IS NULL OR r.distributedAt < :toDate)
        ORDER BY r.distributedAt DESC
    """)
    List<ReliefDistribution> findAllWithinRange(@Param("fromDate") LocalDateTime fromDate,
                                                @Param("toDate") LocalDateTime toDate);

}
