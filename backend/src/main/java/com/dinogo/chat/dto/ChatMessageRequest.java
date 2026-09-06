package com.dinogo.chat.dto;

import com.dinogo.chat.entity.ChatMessageType;

import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        ChatMessageType messageType,
        @Size(max = 2000) String content,
        @Size(max = 500) String imageUrl,
        @Size(max = 255) String imagePublicId,
        Integer productId,
        Integer skuId,
        Integer orderId) {
}
