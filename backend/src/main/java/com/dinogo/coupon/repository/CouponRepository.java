package com.dinogo.coupon.repository;

import com.dinogo.coupon.entity.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findBySellerIdOrderByCouponIdDesc(Long sellerId);

    Optional<Coupon> findBySellerIdAndCouponId(Long sellerId, Long couponId);

    Optional<Coupon> findBySellerIdAndCouponCode(Long sellerId, String couponCode);
}
