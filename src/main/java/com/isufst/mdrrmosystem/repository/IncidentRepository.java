package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    long countByStatus(String status);

    List<Incident> findByStatus(String status);
}
