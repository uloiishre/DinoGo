package com.dinogo.sales.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.dinogo.sales.entity.Order;

/** 訂單聚合的持久化介面。 */
public interface OrderRepository extends JpaRepository<Order, Integer> {
    // 因為 orderItems 是 Lazy Loading，列表或詳情映射 DTO 時可能產生 N+1。用 @EntityGraph
    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Integer buyerId);

    @EntityGraph(attributePaths = { "orderItems", "shipment" })
    Optional<Order> findByOrderIdAndBuyerId(
            Integer orderId,
            Integer buyerId);

    @EntityGraph(attributePaths = { "orderItems", "shipment" })
    Optional<Order> findByOrderIdAndSellerId(
            Integer orderId,
            Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
              AND orders.sellerId = :sellerId
            """)
    Optional<Order> findForShipmentCreation(
            @Param("orderId") Integer orderId,
            @Param("sellerId") Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "orderItems")
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
              AND orders.buyerId = :buyerId
            """)
    Optional<Order> findForCancellation(
            @Param("orderId") Integer orderId,
            @Param("buyerId") Integer buyerId);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findBySellerIdOrderByCreatedAtDesc(Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "payments")
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
              AND orders.buyerId = :buyerId
            """)
    Optional<Order> findForPaymentCreation(
            @Param("orderId") Integer orderId,
            @Param("buyerId") Integer buyerId);
}
