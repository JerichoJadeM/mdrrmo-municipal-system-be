package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Conversation;
import com.isufst.mdrrmosystem.entity.ConversationParticipant;
import com.isufst.mdrrmosystem.entity.Message;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.ConversationParticipantRepository;
import com.isufst.mdrrmosystem.repository.ConversationRepository;
import com.isufst.mdrrmosystem.repository.MessageRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.request.CreateConversationRequest;
import com.isufst.mdrrmosystem.request.PinMessageRequest;
import com.isufst.mdrrmosystem.request.SendMessageRequest;
import com.isufst.mdrrmosystem.request.UpdateMessageRequest;
import com.isufst.mdrrmosystem.response.ConversationSummaryResponse;
import com.isufst.mdrrmosystem.response.MessageResponse;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final NotificationService notificationService;

    public MessageServiceImpl(ConversationRepository conversationRepository, ConversationParticipantRepository conversationParticipantRepository,
                              MessageRepository messageRepository, UserRepository userRepository,
                              FindAuthenticatedUser findAuthenticatedUser, NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.notificationService = notificationService;
    }

    @Override
    public List<ConversationSummaryResponse> getMyConversations() {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        return conversationParticipantRepository.findByUser_Id(currentUser.getId()).stream()
                .map(ConversationParticipant::getConversation).distinct()
                .map(conversation -> mapSummary(conversation, currentUser))
                .sorted(Comparator.comparing(ConversationSummaryResponse::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<MessageResponse> getConversationMessages(Long conversationId) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        assertParticipant(conversationId, currentUser.getId());
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId).stream().map(this::mapMessage).toList();
    }

    @Override @Transactional
    public ConversationSummaryResponse createConversation(CreateConversationRequest request) {
        User sender = findAuthenticatedUser.getAuthenticatedUser();
        User recipient = userRepository.findById(request.recipientUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
        Conversation conversation = conversationRepository.save(new Conversation());

        ConversationParticipant p1 = new ConversationParticipant();
        p1.setConversation(conversation);
        p1.setUser(sender);
        ConversationParticipant p2 = new ConversationParticipant();
        p2.setConversation(conversation);
        p2.setUser(recipient);
        conversationParticipantRepository.saveAll(List.of(p1, p2));

        Message firstMessage = new Message();
        firstMessage.setConversation(conversation);
        firstMessage.setSender(sender);
        firstMessage.setContent(request.message().trim());
        messageRepository.save(firstMessage);

        notificationService.notifyUser(recipient, "MESSAGE", "New Message", "You have a new conversation from " + sender.getFullName(), "CONVERSATION", conversation.getId());
        return mapSummary(conversation, sender);
    }

    @Override @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request) {
        User sender = findAuthenticatedUser.getAuthenticatedUser();
        assertParticipant(conversationId, sender.getId());
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.message().trim());
        Message saved = messageRepository.save(message);

        conversationParticipantRepository.findByConversation_Id(conversationId).stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !(user.getId() == sender.getId()))
                .forEach(user -> notificationService.notifyUser(user, "MESSAGE", "New Message", sender.getFullName() + " sent you a message.", "CONVERSATION", conversationId));

        return mapMessage(saved);
    }

    @Override
    @Transactional
    public MessageResponse updateMessage(Long messageId, UpdateMessageRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!(message.getSender().getId() == currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own messages");
        }

        if (request.content() == null || request.content().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }

        message.setContent(request.content().trim());
        message.setEditedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        return mapMessage(saved);
    }

    @Override
    public void deleteMessage(long messageId) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        boolean isOwner = message.getSender().getId() == currentUser.getId();
        boolean isAdminOrManager = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));

        if (!isOwner && !isAdminOrManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this message");
        }

        messageRepository.delete(message);
    }

    @Override
    @Transactional
    public MessageResponse pinMessage(Long messageId, PinMessageRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        boolean isOwner = message.getSender().getId() == currentUser.getId();
        boolean isAdminOrManager = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));

        if (!isOwner && !isAdminOrManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to pin this message");
        }

        boolean willPin = Boolean.TRUE.equals(request.pinned());

        if (willPin) {
            messageRepository.clearOtherPinnedMessages(message.getConversation().getId(), message.getId());
        }

        message.setPinned(willPin);
        Message saved = messageRepository.save(message);
        return mapMessage(saved);
    }


    private void assertParticipant(Long conversationId, Long userId) {
        boolean allowed = conversationParticipantRepository.findByConversation_Id(conversationId).stream()
                .anyMatch(participant -> participant.getUser().getId() == userId);
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this conversation");
    }

    private ConversationSummaryResponse mapSummary(Conversation conversation, User currentUser) {
        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversation_Id(conversation.getId());
        User otherUser = participants.stream().map(ConversationParticipant::getUser)
                .filter(user -> !(user.getId() == currentUser.getId())).findFirst().orElse(currentUser);
        var lastMessage = messageRepository.findTopByConversation_IdOrderByCreatedAtDesc(conversation.getId()).orElse(null);
        return new ConversationSummaryResponse(conversation.getId(), otherUser.getFullName(),
                lastMessage != null ? lastMessage.getContent() : "",
                lastMessage != null ? lastMessage.getCreatedAt() : conversation.getCreatedAt(), 0);
    }

    private MessageResponse mapMessage(Message message) {
        return new MessageResponse(message.getId(), message.getSender().getId(), message.getSender().getFullName(),
                message.getContent(), message.getCreatedAt(),
                Boolean.TRUE.equals(message.getPinned()),
                message.getEditedAt()
                );
    }
}
