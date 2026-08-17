package com.dinogo.msg.entity;

public enum SellerInbox {

    SYSTEM_NOTICE, // OA、OS
    NEW_ORDER, // AS + PAID
    PROGRESS_ORDER, // AS + SHIPPED / CANCELLED
    COMPLETED_ORDER // AS + COMPLETED
}