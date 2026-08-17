package com.dinogo.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(
		Integer cartItemId,
		Integer skuId,
		String productName,
		BigDecimal price,
		Integer quantity,
		String productImage,
		Integer sellerId,
		String storeName,
		List<SkuOptionResponse> skus) {
}
