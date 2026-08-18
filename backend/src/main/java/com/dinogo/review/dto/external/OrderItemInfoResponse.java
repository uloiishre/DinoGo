package com.dinogo.review.dto.external;

import java.math.BigDecimal;

// 對應 sales 模組 OrderItemResponse。
public record OrderItemInfoResponse(
        Integer orderItemId,
        Integer productId,
        Integer skuId,
        String productName,
        String skuSpec,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Boolean isReviewed) {
}
