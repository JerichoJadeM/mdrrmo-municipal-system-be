package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByUser_Id(Long userId);

    List<ConversationParticipant> findByConversation_Id(Long conversationId);
}