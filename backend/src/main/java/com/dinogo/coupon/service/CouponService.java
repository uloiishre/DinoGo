package com.dinogo.coupon.service;

import com.dinogo.coupon.dto.CouponCreateRequest;
import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.dto.CouponUpdateRequest;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.repository.CouponRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private static final Set<String> DISCOUNT_TYPES = Set.of("PERCENT", "AMOUNT");
    private static final Set<String> SCOPE_TYPES = Set.of("ALL", "CATEGORY", "PRODUCT");

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(Integer sellerId) {
        return couponRepository.findBySellerIdOrderByCouponIdDesc(sellerId)
                .stream()
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Integer sellerId, Integer couponId) {
        return CouponResponse.from(findSellerCoupon(sellerId, couponId));
    }

    @Transactional
    public CouponResponse createCoupon(Integer sellerId, CouponCreateRequest request) {
        validateTimeRange(request.startAt(), request.endAt());
        validateDiscountType(request.discountType());
        validateScopeType(request.scopeType(), request.categoryId(), request.productId());

        couponRepository.findBySellerIdAndCouponCode(sellerId, request.couponCode())
                .ifPresent(coupon -> {
                    throw new IllegalArgumentException("此賣家的優惠券代碼已存在");
                });

        Coupon coupon = new Coupon();
        coupon.setSellerId(sellerId);
        coupon.setCouponCode(request.couponCode());
        coupon.setCouponName(request.couponName());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setLimitCount(request.limitCount());
        coupon.setUsedCount(0);
        coupon.setScopeType(request.scopeType());
        coupon.setCategoryId(request.categoryId());
        coupon.setProductId(request.productId());
        coupon.setStatus("DRAFT");

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse updateCoupon(Integer sellerId, Integer couponId, CouponUpdateRequest request) {
        validateTimeRange(request.startAt(), request.endAt());
        validateDiscountType(request.discountType());
        validateScopeType(request.scopeType(), request.categoryId(), request.productId());

        Coupon coupon = findSellerCoupon(sellerId, couponId);
        coupon.setCouponName(request.couponName());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setLimitCount(request.limitCount());
        coupon.setScopeType(request.scopeType());
        coupon.setCategoryId(request.categoryId());
        coupon.setProductId(request.productId());

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse activateCoupon(Integer sellerId, Integer couponId) {
        Coupon coupon = findSellerCoupon(sellerId, couponId);
        coupon.setStatus("ACTIVE");
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse disableCoupon(Integer sellerId, Integer couponId) {
        Coupon coupon = findSellerCoupon(sellerId, couponId);
        coupon.setStatus("DISABLED");
        return CouponResponse.from(couponRepository.save(coupon));
    }

    private Coupon findSellerCoupon(Integer sellerId, Integer couponId) {
        return couponRepository.findBySellerIdAndCouponId(sellerId, couponId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的優惠券"));
    }

    private void validateTimeRange(java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("結束時間必須在開始時間之後");
        }
    }

    private void validateDiscountType(String discountType) {
        if (!DISCOUNT_TYPES.contains(discountType)) {
            throw new IllegalArgumentException("無效的折扣類型，請選擇 PERCENT 或 AMOUNT");
        }
    }

    private void validateScopeType(String scopeType, Integer categoryId, Integer productId) {
        if (!SCOPE_TYPES.contains(scopeType)) {
            throw new IllegalArgumentException("無效的範圍類型，請選擇 ALL、CATEGORY 或 PRODUCT");
        }
        if ("CATEGORY".equals(scopeType) && categoryId == null) {
            throw new IllegalArgumentException("適用分類優惠券必須提供 categoryId");
        }
        if ("PRODUCT".equals(scopeType) && productId == null) {
            throw new IllegalArgumentException("適用商品優惠券必須提供 productId");
        }
    }
}
