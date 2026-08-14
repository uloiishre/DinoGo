package com.dinogo.sales.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByOrderOrderIdOrderByCreatedAtDesc(Integer orderId);

    boolean existsByOrderOrderIdAndStatus(Integer orderId, PaymentStatus status);
}
