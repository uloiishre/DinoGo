package com.dinogo.port.coupon;

import java.math.BigDecimal;

public record DiscountResult(BigDecimal discountAmount) {

    public DiscountResult {
        if (discountAmount == null) {
            throw new IllegalArgumentException("Discount amount is required");
        }
        if (discountAmount.signum() < 0) {
            throw new IllegalArgumentException("Discount amount must not be negative");
        }
    }
}
