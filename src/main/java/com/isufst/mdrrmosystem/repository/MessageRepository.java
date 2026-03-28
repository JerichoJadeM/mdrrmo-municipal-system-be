package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    Optional<Message> findTopByConversation_IdOrderByCreatedAtDesc(Long conversationId);

    @Modifying
    @Query("""
        update Message m
        set m.pinned = false
        where m.conversation.id = :conversationId
          and m.pinned = true
          and m.id <> :messageId
    """)
    void clearOtherPinnedMessages(@Param("conversationId") Long conversationId,
                                  @Param("messageId") Long messageId);

    Optional<Message> findFirstByConversation_IdAndPinnedTrue(Long conversationId);
}
