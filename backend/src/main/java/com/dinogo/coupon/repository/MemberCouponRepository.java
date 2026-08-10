package com.dinogo.coupon.repository;

import com.dinogo.coupon.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
}
