package com.dinogo.coupon.controller;

import com.dinogo.coupon.dto.CouponCreateRequest;
import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.dto.CouponUpdateRequest;
import com.dinogo.coupon.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public List<CouponResponse> getCoupons(@RequestParam Long sellerId) {
        return couponService.getCoupons(sellerId);
    }

    @GetMapping("/{couponId}")
    public CouponResponse getCoupon(@RequestParam Long sellerId, @PathVariable Long couponId) {
        return couponService.getCoupon(sellerId, couponId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(
            @RequestParam Long sellerId,
            @Valid @RequestBody CouponCreateRequest request
    ) {
        return couponService.createCoupon(sellerId, request);
    }

    @PutMapping("/{couponId}")
    public CouponResponse updateCoupon(
            @RequestParam Long sellerId,
            @PathVariable Long couponId,
            @Valid @RequestBody CouponUpdateRequest request
    ) {
        return couponService.updateCoupon(sellerId, couponId, request);
    }

    @PatchMapping("/{couponId}/activate")
    public CouponResponse activateCoupon(@RequestParam Long sellerId, @PathVariable Long couponId) {
        return couponService.activateCoupon(sellerId, couponId);
    }

    @PatchMapping("/{couponId}/disable")
    public CouponResponse disableCoupon(@RequestParam Long sellerId, @PathVariable Long couponId) {
        return couponService.disableCoupon(sellerId, couponId);
    }
}
