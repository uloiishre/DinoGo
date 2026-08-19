package com.dinogo.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponUsageServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberCouponRepository memberCouponRepository;

    private CouponUsageService couponUsageService;

    @BeforeEach
    void setUp() {
        couponUsageService = new CouponUsageService(
                couponRepository,
                memberCouponRepository);
    }

    @Test
    void calculatesPercentageDiscountForActiveCoupon() {
        MemberCoupon memberCoupon = memberCoupon(10, 7, false);
        Coupon coupon = coupon("ACTIVE", "ALL", "PERCENT", "10", 100);
        when(memberCouponRepository.findByMemberCouponIdAndMemberId(10, 7))
                .thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));

        CouponUsageService.AppliedCoupon applied = couponUsageService.validateAndCalculate(
                10,
                7,
                3,
                new BigDecimal("500.00"),
                List.of(new CouponUsageService.CouponItem(null, new BigDecimal("500.00"))));

        assertThat(applied.discount()).isEqualByComparingTo("50.00");
    }

    @Test
    void capsFixedDiscountAtApplicableAmount() {
        MemberCoupon memberCoupon = memberCoupon(10, 7, false);
        Coupon coupon = coupon("ACTIVE", "ALL", "AMOUNT", "200", 100);
        when(memberCouponRepository.findByMemberCouponIdAndMemberId(10, 7))
                .thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));

        CouponUsageService.AppliedCoupon applied = couponUsageService.validateAndCalculate(
                10,
                7,
                3,
                new BigDecimal("80.00"),
                List.of(new CouponUsageService.CouponItem(null, new BigDecimal("80.00"))));

        assertThat(applied.discount()).isEqualByComparingTo("80.00");
    }

    @Test
    void rejectsUsedCoupon() {
        MemberCoupon memberCoupon = memberCoupon(10, 7, true);
        when(memberCouponRepository.findByMemberCouponIdAndMemberId(10, 7))
                .thenReturn(Optional.of(memberCoupon));

        assertThatThrownBy(() -> couponUsageService.validateAndCalculate(
                10, 7, 3, new BigDecimal("500.00"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("此優惠券已使用");

        verify(couponRepository, never()).findById(any());
    }

    @Test
    void rejectsCouponBelowMinimumPurchaseAmount() {
        MemberCoupon memberCoupon = memberCoupon(10, 7, false);
        Coupon coupon = coupon("ACTIVE", "ALL", "AMOUNT", "50", 100);
        coupon.setMinPurchaseAmount(new BigDecimal("500.00"));
        when(memberCouponRepository.findByMemberCouponIdAndMemberId(10, 7))
                .thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponUsageService.validateAndCalculate(
                10,
                7,
                3,
                new BigDecimal("499.99"),
                List.of(new CouponUsageService.CouponItem(null, new BigDecimal("499.99")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("尚未達到優惠券最低消費金額");
    }

    @Test
    void rejectsStoreCouponForAnotherSeller() {
        MemberCoupon memberCoupon = memberCoupon(10, 7, false);
        Coupon coupon = coupon("ACTIVE", "STORE", "AMOUNT", "50", 100);
        coupon.setSellerId(99);
        when(memberCouponRepository.findByMemberCouponIdAndMemberId(10, 7))
                .thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponUsageService.validateAndCalculate(
                10,
                7,
                3,
                new BigDecimal("500.00"),
                List.of(new CouponUsageService.CouponItem(null, new BigDecimal("500.00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("此優惠券不適用於本次訂單商品");
    }

    @Test
    void consumeMarksMemberCouponUsedAndIncrementsCouponUsage() {
        MemberCoupon memberCoupon = memberCoupon(10, 7, false);
        Coupon coupon = coupon("ACTIVE", "ALL", "AMOUNT", "50", 100);
        CouponUsageService.AppliedCoupon applied = new CouponUsageService.AppliedCoupon(
                memberCoupon,
                coupon,
                new BigDecimal("50.00"));

        couponUsageService.consume(applied);

        assertThat(memberCoupon.getUsed()).isTrue();
        assertThat(memberCoupon.getUsedAt()).isNotNull();
        assertThat(coupon.getUsedCount()).isEqualTo(1);
        verify(memberCouponRepository).save(memberCoupon);
        verify(couponRepository).save(coupon);
    }

    private MemberCoupon memberCoupon(Integer memberCouponId, Integer memberId, boolean used) {
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberCouponId(memberCouponId);
        memberCoupon.setMemberId(memberId);
        memberCoupon.setCouponId(100);
        memberCoupon.setUsed(used);
        return memberCoupon;
    }

    private Coupon coupon(
            String status,
            String scopeType,
            String discountType,
            String discountValue,
            Integer couponId) {
        Coupon coupon = new Coupon();
        coupon.setCouponId(couponId);
        coupon.setSellerId(3);
        coupon.setStatus(status);
        coupon.setScopeType(scopeType);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(new BigDecimal(discountValue));
        coupon.setStartAt(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setUsedCount(0);
        return coupon;
    }
}
