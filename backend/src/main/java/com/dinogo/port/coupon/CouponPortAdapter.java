package com.dinogo.port.coupon;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.repository.CouponRepository;

@Component
public class CouponPortAdapter implements CouponPort {

    private final CouponRepository couponRepository;

    public CouponPortAdapter(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public DiscountResult validateAndReserve(
            Integer memberId,
            Integer couponId,
            Integer sellerId,
            BigDecimal subtotal) {
        if (couponId == null || couponId <= 0) {
            return new DiscountResult(BigDecimal.ZERO);
        }

        Coupon coupon = couponRepository.findById(couponId.longValue())
                .orElseThrow(() -> new IllegalArgumentException("Coupon does not exist: " + couponId));

        if (!sellerId.equals(coupon.getSellerId())) {
            throw new IllegalArgumentException("Coupon is not valid for seller: " + sellerId);
        }
        if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
            throw new IllegalArgumentException("Coupon is not active: " + couponId);
        }

        BigDecimal minimumPurchaseAmount = Optional.ofNullable(coupon.getMinPurchaseAmount())
                .orElse(BigDecimal.ZERO);
        if (subtotal.compareTo(minimumPurchaseAmount) < 0) {
            return new DiscountResult(BigDecimal.ZERO);
        }

        BigDecimal discountAmount = switch (coupon.getDiscountType()) {
            case "PERCENT" -> subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            case "AMOUNT" -> coupon.getDiscountValue();
            default -> BigDecimal.ZERO;
        };

        return new DiscountResult(discountAmount.min(subtotal));
    }

    @Override
    public void release(Integer memberId, Integer couponId, Integer orderId) {
        // Coupon release is not implemented in this minimal adapter yet.
    }
}
