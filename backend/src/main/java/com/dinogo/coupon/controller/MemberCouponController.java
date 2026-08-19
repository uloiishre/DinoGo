package com.dinogo.coupon.controller;

import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.service.MemberCouponService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/coupons")
public class MemberCouponController {

    private final MemberCouponService memberCouponService;

    public MemberCouponController(MemberCouponService memberCouponService) {
        this.memberCouponService = memberCouponService;
    }

    @PostMapping("/{couponId}/claim")
    public CouponResponse claimCoupon(
            @RequestParam Integer memberId,
            @PathVariable Integer couponId) {
        return memberCouponService.claimCoupon(memberId, couponId);
    }
}