package com.dinogo.adapter.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.dinogo.port.coupon.CouponPort;
import com.dinogo.port.coupon.DiscountResult;

@Component
public class NoCouponAdapter implements CouponPort {

    @Override
    public DiscountResult validateAndReserve(
            Integer memberId,
            Integer couponId,
            Integer sellerId,
            BigDecimal subtotal) {
        return new DiscountResult(BigDecimal.ZERO);
    }

    @Override
    public void release(Integer memberId, Integer couponId, Integer orderId) {
        // No coupon is reserved by this temporary adapter.
    }
}
