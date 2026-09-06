package com.dinogo.chat.dto;

import java.time.LocalDateTime;

public record ChatConversationResponse(
        Integer conversationId,
        Integer buyerId,
        Integer sellerId,
        String sellerName,
        String sellerLogoUrl,
        String buyerName,
        String latestMessage,
        LocalDateTime latestMessageAt,
        Integer unreadCount) {
}
