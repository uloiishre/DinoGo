package com.dinogo.sales.dto.order;

import com.dinogo.sales.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;

/** 訂單狀態更新請求；實際允許的轉換仍由 Service 狀態機驗證。 */
public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status
) {
}
