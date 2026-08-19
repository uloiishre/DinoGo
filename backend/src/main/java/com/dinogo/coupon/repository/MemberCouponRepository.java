package com.dinogo.coupon.repository;

import com.dinogo.coupon.entity.MemberCoupon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// ID 是 Integer，要和 Entity 的 memberCouponId 一致
public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Integer> {

    // 檢查會員是否已領過這張優惠券
    boolean existsByMemberIdAndCouponId(Integer memberId, Integer couponId);

    // 之後查會員某張券可用
    Optional<MemberCoupon> findByMemberIdAndCouponId(Integer memberId, Integer couponId);
}
