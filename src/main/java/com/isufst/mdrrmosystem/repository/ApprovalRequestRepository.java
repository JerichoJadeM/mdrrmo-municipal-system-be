package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<ApprovalRequest> findByRequestedBy_IdOrderByCreatedAtDesc(Long userId);

    Optional<ApprovalRequest> findTopByRequestTypeAndReferenceTypeAndReferenceIdAndRequestedBy_IdAndStatusOrderByCreatedAtDesc(
            String requestType,
            String referenceType,
            Long referenceId,
            Long requestedById,
            String status
    );

    boolean existsByRequestTypeAndReferenceTypeAndReferenceIdAndRequestedBy_IdAndStatus(
            String requestType,
            String referenceType,
            Long referenceId,
            Long requestedById,
            String status
    );

    Optional<ApprovalRequest> findTopByRequestTypeAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            String requestType,
            String referenceType,
            Long referenceId
    );

    @Query("""
        SELECT ar
        FROM ApprovalRequest ar
        WHERE UPPER(ar.requestType) = UPPER(:requestType)
          AND UPPER(ar.referenceType) = UPPER(:referenceType)
          AND ar.referenceId = :referenceId
          AND (
                :mode IS NULL
                OR ar.payloadJson LIKE CONCAT('%\"mode\":\"', :mode, '\"%')
              )
        ORDER BY ar.createdAt DESC
    """)
    List<ApprovalRequest> findLatestByRequestTypeReferenceAndMode(@Param("requestType") String requestType,
                                                                  @Param("referenceType") String referenceType,
                                                                  @Param("referenceId") Long referenceId,
                                                                  @Param("mode") String mode);

    @Query("""
        SELECT COUNT(ar) > 0
        FROM ApprovalRequest ar
        WHERE UPPER(ar.requestType) = UPPER(:requestType)
          AND UPPER(ar.referenceType) = UPPER(:referenceType)
          AND ar.referenceId = :referenceId
          AND UPPER(ar.status) = 'PENDING'
          AND (
                :mode IS NULL
                OR ar.payloadJson LIKE CONCAT('%\"mode\":\"', :mode, '\"%')
              )
    """)
    boolean existsPendingByRequestTypeReferenceAndMode(@Param("requestType") String requestType,
                                                       @Param("referenceType") String referenceType,
                                                       @Param("referenceId") Long referenceId,
                                                       @Param("mode") String mode);

    @Query("""
        SELECT COUNT(ar) > 0
        FROM ApprovalRequest ar
        WHERE UPPER(ar.requestType) = 'OPERATION_ACKNOWLEDGEMENT'
          AND UPPER(ar.referenceType) = UPPER(:referenceType)
          AND ar.referenceId = :referenceId
          AND UPPER(ar.status) = 'APPROVED'
          AND (
                :mode IS NULL
                OR ar.payloadJson LIKE CONCAT('%\"mode\":\"', :mode, '\"%')
              )
    """)
    boolean existsApprovedOperationAcknowledgement(@Param("referenceType") String referenceType,
                                                   @Param("referenceId") Long referenceId,
                                                   @Param("mode") String mode);

    @Query("""
        SELECT ar
        FROM ApprovalRequest ar
        WHERE (:actionType IS NULL OR UPPER(ar.status) = :actionType OR UPPER(ar.requestType) = :actionType)
          AND (:performedBy IS NULL OR (
                (ar.requestedBy IS NOT NULL AND UPPER(CONCAT(COALESCE(ar.requestedBy.firstName, ''), ' ', COALESCE(ar.requestedBy.lastName, ''))) LIKE CONCAT('%', :performedBy, '%'))
                OR
                (ar.reviewedBy IS NOT NULL AND UPPER(CONCAT(COALESCE(ar.reviewedBy.firstName, ''), ' ', COALESCE(ar.reviewedBy.lastName, ''))) LIKE CONCAT('%', :performedBy, '%'))
              ))
          AND (:recordId IS NULL OR ar.id = :recordId OR ar.referenceId = :recordId)
          AND (:fromDate IS NULL OR ar.createdAt >= :fromDate)
          AND (:toDate IS NULL OR ar.createdAt < :toDate)
        ORDER BY ar.createdAt DESC
    """)
    List<ApprovalRequest> searchAuditTrail(@Param("actionType") String actionType,
                                           @Param("performedBy") String performedBy,
                                           @Param("recordId") Long recordId,
                                           @Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate);

}
