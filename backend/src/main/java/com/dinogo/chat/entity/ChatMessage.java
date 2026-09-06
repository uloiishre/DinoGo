package com.dinogo.chat.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    private Integer messageId;

    private ChatConversation conversation;

    private Integer senderMemberId;

    private ChatSenderRole senderRole;

    private ChatMessageType messageType = ChatMessageType.TEXT;

    private String content;

    private String imageUrl;

    private String imagePublicId;

    private Integer productId;

    private Integer skuId;

    private Integer orderId;

    private LocalDateTime createdAt;
}
