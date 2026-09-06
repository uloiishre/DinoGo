package com.dinogo.chat.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class DinoChatWebSocketSessions {

    private final ConcurrentHashMap<Integer, Set<WebSocketSession>> sessionsByMemberId = new ConcurrentHashMap<>();

    public void add(Integer memberId, WebSocketSession session) {
        sessionsByMemberId.computeIfAbsent(memberId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(WebSocketSession session) {
        sessionsByMemberId.values().forEach(sessions -> sessions.remove(session));
    }

    public void sendToMembers(List<Integer> memberIds, String payload) {
        TextMessage message = new TextMessage(payload);
        for (Integer memberId : memberIds) {
            for (WebSocketSession session : sessionsByMemberId.getOrDefault(memberId, Set.of())) {
                if (!session.isOpen()) continue;
                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                } catch (IOException ignored) {
                    remove(session);
                }
            }
        }
    }
}
