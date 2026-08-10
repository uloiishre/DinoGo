package com.dinogo.sales.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 建立訂單時的一筆 SKU 與購買數量。 */
public record CreateOrderItemRequest(
        @NotNull Integer skuId,
        @NotNull @Min(1) Integer quantity
) {
}
