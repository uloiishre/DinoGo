package com.dinogo.sales.dto.order;

import java.math.BigDecimal;

import com.dinogo.sales.entity.OrderStatus;

/** 建立訂單成功後回傳的識別資訊與金額計算結果。 */
public record CreateOrderResponse(
        Integer orderId,
        String orderNo,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {
}
