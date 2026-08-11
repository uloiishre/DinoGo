package com.dinogo.sales.entity;

/** 訂單生命週期狀態；狀態轉換規則由 OrderService 統一控制。 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}
