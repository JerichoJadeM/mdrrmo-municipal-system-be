package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
