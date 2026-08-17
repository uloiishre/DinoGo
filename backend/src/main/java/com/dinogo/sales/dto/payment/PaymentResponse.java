package com.dinogo.sales.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dinogo.sales.entity.PaymentStatus;

public record PaymentResponse(
        Integer paymentId,
        String paymentNo,
        Integer orderId,
        String paymentMethodCode,
        BigDecimal amount,
        PaymentStatus status,
        String transactionNo,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime createdAt) {
}
