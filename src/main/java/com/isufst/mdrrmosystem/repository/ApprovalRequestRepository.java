package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}
