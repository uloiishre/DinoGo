package com.dinogo.sales.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sales.entity.Order;

/** 訂單聚合的持久化介面。 */
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /** 查詢會員訂單並預先載入明細，避免逐筆載入造成 N+1 查詢。 */
    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Integer buyerId);
}
