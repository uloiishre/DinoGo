package com.dinogo.chat.dto;

public record ChatContextRequest(
        Integer sellerId,
        Integer productId,
        Integer skuId,
        Integer orderId) {
}
