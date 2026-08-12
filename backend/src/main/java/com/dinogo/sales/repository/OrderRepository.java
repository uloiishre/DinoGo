package com.dinogo.sales.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sales.entity.Order;

/** 訂單聚合的持久化介面。 */
public interface OrderRepository extends JpaRepository<Order, Integer> {
    // 因為 orderItems 是 Lazy Loading，列表或詳情映射 DTO 時可能產生 N+1。用 @EntityGraph
    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Integer buyerId);
    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findByOrderIdAndBuyerId(
            Integer orderId,
            Integer buyerId);
}
