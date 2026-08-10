package com.dinogo.sales.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dinogo.sales.entity.OrderStatus;

/** 訂單列表使用的精簡回應，保留列表呈現所需的基本資料與商品明細。 */
public record OrderSummaryResponse(
        Integer orderId,
        String orderNo,
        Integer sellerId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
