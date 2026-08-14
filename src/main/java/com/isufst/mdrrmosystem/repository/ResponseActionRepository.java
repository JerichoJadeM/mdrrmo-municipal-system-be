package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ResponseAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResponseActionRepository extends JpaRepository<ResponseAction, Long> {

    List<ResponseAction> findByIncident_Id(Long incidentId);

    List<ResponseAction> findByIncidentIdOrderByActionTimeDesc(Long incidentId);

    Optional<ResponseAction> findTopByIncidentIdOrderByActionTimeDesc(Long incidentId);

    @Modifying
    @Query("update ResponseAction r set r.responder = null where r.responder.id = :userId")
    void detachResponder(@Param("userId") Long userId);
}
