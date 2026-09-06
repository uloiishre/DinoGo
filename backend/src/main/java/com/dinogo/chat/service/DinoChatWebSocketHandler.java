package com.dinogo.chat.service;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.dinogo.chat.dto.ChatMessageRequest;
import com.dinogo.chat.dto.ChatMessageResponse;
import com.dinogo.chat.entity.ChatMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class DinoChatWebSocketHandler extends TextWebSocketHandler {

    private static final String MEMBER_ID_ATTRIBUTE = "memberId";

    private final DinoChatWebSocketAuthService authService;
    private final DinoChatWebSocketSessions sessions;
    private final DinoChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public DinoChatWebSocketHandler(
            DinoChatWebSocketAuthService authService,
            DinoChatWebSocketSessions sessions,
            DinoChatService chatService) {
        this.authService = authService;
        this.sessions = sessions;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Integer memberId = authService.authenticate(session.getUri());
            session.getAttributes().put(MEMBER_ID_ATTRIBUTE, memberId);
            sessions.add(memberId, session);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "CONNECTED"))));
        } catch (RuntimeException exception) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Integer memberId = (Integer) session.getAttributes().get(MEMBER_ID_ATTRIBUTE);
        if (memberId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        try {
            IncomingMessage incoming = objectMapper.readValue(textMessage.getPayload(), IncomingMessage.class);
            ChatMessageRequest request = new ChatMessageRequest(
                    incoming.messageType(),
                    incoming.content(),
                    incoming.imageUrl(),
                    incoming.imagePublicId(),
                    incoming.productId(),
                    incoming.skuId(),
                    incoming.orderId());
            ChatMessageResponse saved = chatService.sendMessage(memberId, incoming.conversationId(), request);
            String payload = objectMapper.writeValueAsString(Map.of("type", "MESSAGE", "message", saved));
            sessions.sendToMembers(chatService.participantMemberIds(incoming.conversationId()), payload);
        } catch (RuntimeException exception) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "ERROR",
                    "message", exception.getMessage() == null ? "訊息傳送失敗。" : exception.getMessage()))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    private record IncomingMessage(
            Integer conversationId,
            ChatMessageType messageType,
            String content,
            String imageUrl,
            String imagePublicId,
            Integer productId,
            Integer skuId,
            Integer orderId) {
    }
}
