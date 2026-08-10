package com.dinogo.sales.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 訂單商品明細回應；商品描述與單價均為下單當下的快照。 */
public record OrderItemResponse(
        Integer orderItemId,
        Integer productId,
        Integer skuId,
        String productName,
        String skuSpec,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Boolean isReviewed,
        LocalDateTime createdAt
) {
}
