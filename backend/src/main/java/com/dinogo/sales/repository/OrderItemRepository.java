package com.dinogo.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sales.entity.OrderItem;

/** 訂單明細的持久化介面；目前使用 Spring Data 提供的基本 CRUD。 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}
