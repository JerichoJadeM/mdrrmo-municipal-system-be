package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvacuationActivationRepository extends JpaRepository<EvacuationActivation, Long> {

    List<EvacuationActivation> findByIncident_Id(Long incidentId);
    List<EvacuationActivation> findByCalamity_Id(Long calamityId);
    List<EvacuationActivation> findByStatus(String status);

    @Modifying
    @Query("DELETE FROM EvacuationActivation ea WHERE ea.incident.id = :incidentId")
    void deleteByIncidentId(@Param("incidentId") Long incidentId);

    @Modifying
    @Query("DELETE FROM EvacuationActivation ea WHERE ea.calamity.id = :calamityId")
    void deleteByCalamityId(@Param("calamityId") Long calamityId);
}
