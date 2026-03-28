package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.CreateConversationRequest;
import com.isufst.mdrrmosystem.request.PinMessageRequest;
import com.isufst.mdrrmosystem.request.SendMessageRequest;
import com.isufst.mdrrmosystem.request.UpdateMessageRequest;
import com.isufst.mdrrmosystem.response.ConversationSummaryResponse;
import com.isufst.mdrrmosystem.response.MessageResponse;
import com.isufst.mdrrmosystem.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;
    public MessageController(MessageService messageService) { this.messageService = messageService; }

    @GetMapping("/conversations")
    public List<ConversationSummaryResponse> getMyConversations() { return messageService.getMyConversations(); }

    @GetMapping("/conversations/{conversationId}")
    public List<MessageResponse> getConversationMessages(@PathVariable Long conversationId) {
        return messageService.getConversationMessages(conversationId);
    }

    @PostMapping("/conversations")
    public ConversationSummaryResponse createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return messageService.createConversation(request);
    }

    @PostMapping("/conversations/{conversationId}")
    public MessageResponse sendMessage(@PathVariable Long conversationId, @Valid @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(conversationId, request);
    }

    @PutMapping("/{messageId}")
    public MessageResponse updateMessage(@PathVariable Long messageId,
                                         @RequestBody UpdateMessageRequest request) {
        return messageService.updateMessage(messageId, request);
    }

    @DeleteMapping("/{messageId}")
    public void deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
    }

    @PutMapping("/{messageId}/pin")
    public MessageResponse pinMessage(@PathVariable Long messageId,
                                      @RequestBody PinMessageRequest request) {
        return messageService.pinMessage(messageId, request);
    }
}
