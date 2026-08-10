package com.dinogo.coupon.dto;

import com.dinogo.coupon.entity.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long couponId,
        Long sellerId,
        String couponCode,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minPurchaseAmount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer limitCount,
        Integer usedCount,
        String scopeType,
        Long categoryId,
        Long productId,
        String status
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getCouponId(),
                coupon.getSellerId(),
                coupon.getCouponCode(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinPurchaseAmount(),
                coupon.getStartAt(),
                coupon.getEndAt(),
                coupon.getLimitCount(),
                coupon.getUsedCount(),
                coupon.getScopeType(),
                coupon.getCategoryId(),
                coupon.getProductId(),
                coupon.getStatus()
        );
    }
}
