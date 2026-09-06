package com.dinogo.chat.dto;

import java.time.LocalDateTime;

public record ChatWebSocketTicketResponse(
        String ticket,
        LocalDateTime expiresAt) {
}
