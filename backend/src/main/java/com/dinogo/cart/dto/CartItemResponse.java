package com.dinogo.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
		Integer cartItemId,
		Integer skuId,
		String productName,
		BigDecimal price,
		Integer quantity,
		String productImage) {
}
