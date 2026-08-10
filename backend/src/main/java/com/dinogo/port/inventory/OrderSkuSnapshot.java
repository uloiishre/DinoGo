package com.dinogo.port.inventory;

import java.math.BigDecimal;

public record OrderSkuSnapshot(
        Integer skuId,
        Integer productId,
        Integer sellerId,
        String productName,
        String skuSpec,
        String productImageUrl,
        BigDecimal unitPrice
) {
}
