package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.request.CreateConversationRequest;
import com.isufst.mdrrmosystem.request.PinMessageRequest;
import com.isufst.mdrrmosystem.request.SendMessageRequest;
import com.isufst.mdrrmosystem.request.UpdateMessageRequest;
import com.isufst.mdrrmosystem.response.ConversationSummaryResponse;
import com.isufst.mdrrmosystem.response.MessageResponse;

import java.util.List;

public interface MessageService {
    List<ConversationSummaryResponse> getMyConversations();
    List<MessageResponse> getConversationMessages(Long conversationId);
    ConversationSummaryResponse createConversation(CreateConversationRequest request);
    MessageResponse sendMessage(Long conversationId, SendMessageRequest request);

    MessageResponse updateMessage(Long messageId, UpdateMessageRequest request);
    void deleteMessage(long messageId);
    MessageResponse pinMessage(Long messageId, PinMessageRequest request);
}
