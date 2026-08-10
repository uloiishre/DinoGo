package com.dinogo.port.coupon;

import java.math.BigDecimal;

public interface CouponPort {

    DiscountResult validateAndReserve(
            Integer memberId,
            Integer couponId,
            Integer sellerId,
            BigDecimal subtotal);

    void release(
            Integer memberId,
            Integer couponId,
            Integer orderId);
}
