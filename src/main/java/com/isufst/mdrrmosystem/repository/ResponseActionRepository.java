package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ResponseAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseActionRepository extends JpaRepository<ResponseAction, Long> {

    List<ResponseAction> findByIncident_Id(Long incidentId);
}
