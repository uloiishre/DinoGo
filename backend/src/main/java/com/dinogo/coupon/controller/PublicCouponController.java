package com.dinogo.coupon.controller;

import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.service.CouponService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class PublicCouponController {

    private final CouponService couponService;

    public PublicCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/available")
    public List<CouponResponse> getAvailableCoupons() {
        return couponService.getAvailableCoupons();
    }
}