package com.dinogo.sales.entity;

/** 單次付款嘗試的處理狀態。 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}
