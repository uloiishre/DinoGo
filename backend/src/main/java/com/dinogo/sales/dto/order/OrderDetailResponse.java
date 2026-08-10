package com.dinogo.sales.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dinogo.sales.entity.OrderStatus;

/** 訂單詳情回應，包含收件快照、金額、取消資訊及完整商品明細。 */
public record OrderDetailResponse(
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
        OrderStatus status,
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
        List<OrderItemResponse> items
) {
}
