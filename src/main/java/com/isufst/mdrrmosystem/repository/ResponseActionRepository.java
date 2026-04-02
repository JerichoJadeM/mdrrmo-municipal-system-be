package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ResponseAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResponseActionRepository extends JpaRepository<ResponseAction, Long> {

    List<ResponseAction> findByIncident_Id(Long incidentId);

    List<ResponseAction> findByIncidentIdOrderByActionTimeDesc(Long incidentId);

    Optional<ResponseAction> findTopByIncidentIdOrderByActionTimeDesc(Long incidentId);
}
