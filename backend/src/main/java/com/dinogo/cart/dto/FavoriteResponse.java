package com.dinogo.cart.dto;

public record FavoriteResponse(
		Integer favoriteId,
        Integer productId,
        String productName,
        Integer price,
        String imageUrl) {

}
