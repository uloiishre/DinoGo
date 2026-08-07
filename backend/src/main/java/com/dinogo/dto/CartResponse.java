package com.dinogo.dto;

import java.util.List;

public record CartResponse(
		Integer cartId, 
		List<CartItemResponse> items) {
}
