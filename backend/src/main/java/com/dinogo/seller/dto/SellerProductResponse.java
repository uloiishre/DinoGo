package com.dinogo.seller.dto;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import java.math.BigDecimal;

public record SellerProductResponse(
        Integer productId,
        Integer sellerId,
        String productName,
        BigDecimal basePrice,
        Integer stock,
        String status
) {

    public static SellerProductResponse from(Product product) {
        Integer stock = product.getSkus()
                .stream()
                .map(ProductSku::getStock)
                .filter(value -> value != null)
                .reduce(0, Integer::sum);

        return new SellerProductResponse(
                product.getProductId(),
                product.getSellerId(),
                product.getProductName(),
                product.getBasePrice(),
                stock,
                statusLabel(product.getStatus())
        );
    }

    private static String statusLabel(Byte status) {
        if (status == null) {
            return "DRAFT";
        }
        return switch (status) {
            case 1 -> "ACTIVE";
            case 2 -> "INACTIVE";
            default -> "DRAFT";
        };
    }
}
