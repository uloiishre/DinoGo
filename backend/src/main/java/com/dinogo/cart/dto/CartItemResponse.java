package com.dinogo.cart.dto;

public record CartItemResponse(
		Integer cartItemId,
		Integer skuId,
//	    String name,
//	    Integer price,
	    Integer quantity
) {

}
