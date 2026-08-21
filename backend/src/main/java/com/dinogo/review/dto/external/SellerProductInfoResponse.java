package com.dinogo.review.dto.external;

import java.math.BigDecimal;

// 對應 seller 模組 SellerProductResponse。
public record SellerProductInfoResponse(
        Integer productId,
        Integer sellerId,
        String productName,
        BigDecimal basePrice,
        Integer stock,
        String status) {
}
