package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    long countByStatus(String status);

    List<Incident> findByStatus(String status);

    @Query("""
        SELECT i.barangay.id, i.barangay.name, COUNT(i)
        FROM Incident i
        GROUP BY i.barangay.id, i.barangay.name
        ORDER BY COUNT(i) DESC
    """)
    List<Object[]>  incidentHeatmap();

    long countByBarangayIdAndStatus(long barangayId, String status);


}
