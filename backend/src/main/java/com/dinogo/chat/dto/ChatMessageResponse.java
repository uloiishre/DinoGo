package com.dinogo.chat.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dinogo.chat.entity.ChatMessageType;
import com.dinogo.chat.entity.ChatSenderRole;

public record ChatMessageResponse(
        Integer messageId,
        Integer conversationId,
        Integer senderMemberId,
        ChatSenderRole senderRole,
        ChatMessageType messageType,
        String content,
        String imageUrl,
        Integer productId,
        Integer skuId,
        ProductCard product,
        Integer orderId,
        OrderCard order,
        LocalDateTime createdAt) {

    public record ProductCard(
            Integer productId,
            Integer skuId,
            String productName,
            String skuText,
            BigDecimal price,
            String imageUrl) {
    }

    public record OrderCard(
            Integer orderId,
            String orderNo,
            String status,
            BigDecimal totalAmount) {
    }
}
