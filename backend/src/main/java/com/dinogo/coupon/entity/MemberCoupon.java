package com.dinogo.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(schema = "seller", name = "MemberCoupon")
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Integer memberCouponId;

    @Column(name = "coupon_id", nullable = false)
    private Integer couponId;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @Column(name = "is_used", nullable = false)
    private Boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}
