package com.dinogo.chat.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatConversation {

    private Integer conversationId;

    private Integer buyerId;

    private Integer sellerId;

    private Integer buyerUnreadCount = 0;

    private Integer sellerUnreadCount = 0;

    private ChatMessage lastMessage;

    private LocalDateTime latestMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
