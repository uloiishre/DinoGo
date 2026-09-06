package com.dinogo.chat.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dinogo.chat.dto.ChatContextRequest;
import com.dinogo.chat.dto.ChatConversationResponse;
import com.dinogo.chat.dto.ChatMessageResponse;
import com.dinogo.chat.dto.ChatUnreadCountResponse;
import com.dinogo.chat.dto.ChatWebSocketTicketResponse;
import com.dinogo.chat.service.DinoChatService;
import com.dinogo.chat.service.DinoChatWebSocketAuthService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.sysmsg.dto.response.SysmsgImageAssetResponse;
import com.dinogo.sysmsg.service.SysmsgImageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class DinoChatController {

    private final DinoChatService chatService;
    private final DinoChatWebSocketAuthService webSocketAuthService;
    private final SysmsgImageService imageService;

    public DinoChatController(
            DinoChatService chatService,
            DinoChatWebSocketAuthService webSocketAuthService,
            SysmsgImageService imageService) {
        this.chatService = chatService;
        this.webSocketAuthService = webSocketAuthService;
        this.imageService = imageService;
    }

    @GetMapping("/conversations")
    public List<ChatConversationResponse> listConversations(@AuthenticationPrincipal AuthenticatedMember member) {
        return chatService.listConversations(member.memberId());
    }

    @PostMapping("/conversations")
    public ChatConversationResponse getOrCreateConversation(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody ChatContextRequest request) {
        return chatService.getOrCreateConversation(member.memberId(), request);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageResponse> listMessages(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer conversationId) {
        return chatService.listMessages(member.memberId(), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/open")
    public ChatConversationResponse openConversation(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer conversationId) {
        return chatService.openConversation(member.memberId(), conversationId);
    }

    @GetMapping("/unread-count")
    public ChatUnreadCountResponse unreadCount(@AuthenticationPrincipal AuthenticatedMember member) {
        return new ChatUnreadCountResponse(chatService.getTotalUnread(member.memberId()));
    }

    @PostMapping("/ws-ticket")
    public ChatWebSocketTicketResponse issueWebSocketTicket(@AuthenticationPrincipal AuthenticatedMember member) {
        return webSocketAuthService.issueTicket(member.memberId());
    }

    @PostMapping("/images")
    public List<SysmsgImageAssetResponse> uploadImages(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestPart("files") List<MultipartFile> files) {
        return imageService.upload(files, member.memberId());
    }
}
