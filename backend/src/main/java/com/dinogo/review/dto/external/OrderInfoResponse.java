package com.dinogo.review.dto.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 對應 sales 模組 OrderDetailResponse；status 使用 String 以降低跨模組 enum 耦合。
public record OrderInfoResponse(
        Integer orderId,
        String orderNo,
        Integer buyerId,
        Integer sellerId,
        Integer addressId,
        String receiverName,
        String receiverPhone,
        String shippingPostalCode,
        String shippingCity,
        String shippingDistrict,
        String shippingDetailAddress,
        String status,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String buyerRemark,
        String cancelReason,
        String cancelledBy,
        LocalDateTime cancelledAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemInfoResponse> items) {
}
