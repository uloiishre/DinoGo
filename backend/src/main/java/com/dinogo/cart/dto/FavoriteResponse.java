package com.dinogo.cart.dto;

import java.math.BigDecimal;

public record FavoriteResponse(
                Integer favoriteId,
                Integer productId,
                Integer skuId,
                String productName,
                BigDecimal basePrice,
                String imageUrl) {
}
