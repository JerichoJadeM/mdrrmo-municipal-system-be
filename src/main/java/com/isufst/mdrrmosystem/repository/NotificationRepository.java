package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long userId);

    long countByRecipient_IdAndIsReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.recipient.id = :userId and n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Notification n where n.recipient.id = :userId and n.isRead = true")
    void deleteReadByUserId(@Param("userId") Long userId);
}