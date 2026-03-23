package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.ResponseAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("""
        SELECT COUNT(i)
        FROM Incident i
        WHERE (:fromDate IS NULL OR i.reportedAt >= :fromDate)
          AND (:toDate IS NULL OR i.reportedAt < :toDate)
    """)
    long countAllWithin(@Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate);

    @Query("""
        SELECT COUNT(i)
        FROM Incident i
        WHERE i.status = :status
          AND (:fromDate IS NULL OR i.reportedAt >= :fromDate)
          AND (:toDate IS NULL OR i.reportedAt < :toDate)
    """)
    long countByStatusWithin(@Param("status") String status,
                             @Param("fromDate") LocalDateTime fromDate,
                             @Param("toDate") LocalDateTime toDate);

    @Query("""
        SELECT i
        FROM Incident i
        WHERE (:fromDate IS NULL OR i.reportedAt >= :fromDate)
          AND (:toDate IS NULL OR i.reportedAt < :toDate)
        ORDER BY i.reportedAt DESC
    """)
    List<Incident> findAllWithin(@Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate);
}
