package com.dinogo.sales.entity;

/** 訂單物流從備貨到送達的處理狀態。 */
public enum ShipmentStatus {
    PREPARING,
    SHIPPED,
    AVAILABLE_FOR_PICKUP,
    DELIVERED
}
