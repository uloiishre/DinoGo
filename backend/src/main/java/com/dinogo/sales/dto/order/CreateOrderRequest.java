package com.dinogo.sales.dto.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 建立訂單請求；收件欄位會保存為歷史快照，items 則由後端重新驗證價格與庫存。
 */
public record CreateOrderRequest(
        @NotNull Integer buyerId,
        Integer addressId,
        @NotBlank @Size(max = 100) String receiverName,
        @NotBlank @Size(max = 20) String receiverPhone,
        @Size(max = 10) String shippingPostalCode,
        @NotBlank @Size(max = 50) String shippingCity,
        @NotBlank @Size(max = 50) String shippingDistrict,
        @NotBlank @Size(max = 255) String shippingDetailAddress,
        @Size(max = 500) String buyerRemark,
        Integer couponId,
        // 有提供購物車項目時，訂單建立成功後才會要求購物車模組清除這些項目。
        List<Integer> cartItemIds,
        @NotEmpty List<@Valid CreateOrderItemRequest> items
) {
}
