package com.dinogo.coupon.service;

import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberCouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    public MemberCouponService(
            CouponRepository couponRepository,
            MemberCouponRepository memberCouponRepository) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
    }

    @Transactional
    public CouponResponse claimCoupon(Integer memberId, Integer couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("找不到優惠券"));

        validateCouponCanBeClaimed(coupon);

        boolean alreadyClaimed = memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId);
        if (alreadyClaimed) {
            throw new IllegalArgumentException("此會員已領取過這張優惠券");
        }

        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberId(memberId);
        memberCoupon.setCouponId(couponId);
        memberCoupon.setUsed(false);
        memberCoupon.setReceivedAt(LocalDateTime.now());

        memberCouponRepository.save(memberCoupon);

        return CouponResponse.from(coupon);
    }

    private void validateCouponCanBeClaimed(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();

        if (!"ACTIVE".equals(coupon.getStatus())) {
            throw new IllegalArgumentException("此優惠券尚未啟用");
        }

        if (coupon.getStartAt().isAfter(now) || coupon.getEndAt().isBefore(now)) {
            throw new IllegalArgumentException("此優惠券不在可領取時間內");
        }

        if (coupon.getLimitCount() != null && coupon.getUsedCount() >= coupon.getLimitCount()) {
            throw new IllegalArgumentException("此優惠券已達領取上限");
        }
    }
}