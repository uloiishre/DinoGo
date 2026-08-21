package com.dinogo.sysmsg.entity;

public enum SellerInbox {

    SYSTEM_NOTICE,        // OA、OS
    NEW_ORDER,            // AS + PAID + SHIPPED + COMPLETED
    CANCELLED_ORDER       // AS + CANCELLED 
}
