package com.dinogo.port.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;

@Component
public class ProductInventoryPortAdapter implements ProductInventoryPort {

    private final ProductSkuRepository productSkuRepository;

    public ProductInventoryPortAdapter(ProductSkuRepository productSkuRepository) {
        this.productSkuRepository = productSkuRepository;
    }

    @Override
    @Transactional
    public List<OrderSkuSnapshot> validateAndDeduct(Map<Integer, Integer> quantitiesBySku) {
        if (quantitiesBySku == null || quantitiesBySku.isEmpty()) {
            return List.of();
        }

        List<OrderSkuSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : quantitiesBySku.entrySet()) {
            Integer skuId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductSku sku = productSkuRepository.findByIdForUpdate(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("SKU does not exist: " + skuId));

            if (sku.getStatus() == null || sku.getStatus() != 1) {
                throw new IllegalArgumentException("SKU is not available: " + skuId);
            }
            Integer currentStock = sku.getStock();
            if (currentStock == null || currentStock < quantity) {
                throw new IllegalArgumentException("Insufficient stock for SKU: " + skuId);
            }

            Product product = sku.getProduct();
            if (product == null) {
                throw new IllegalArgumentException("Product does not exist for SKU: " + skuId);
            }

            sku.setStock(currentStock - quantity);
            productSkuRepository.save(sku);

            snapshots.add(new OrderSkuSnapshot(
                    sku.getSkuId(),
                    product.getProductId(),
                    product.getSeller() != null ? product.getSeller().getSellerId() : null,
                    product.getProductName(),
                    buildSkuSpec(sku),
                    resolveMainImageUrl(product),
                    sku.getPrice()));
        }

        return snapshots;
    }

    @Override
    @Transactional
    public void restore(Map<Integer, Integer> quantitiesBySku) {
        if (quantitiesBySku == null || quantitiesBySku.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> entry : quantitiesBySku.entrySet()) {
            Integer skuId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductSku sku = productSkuRepository.findByIdForUpdate(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("SKU does not exist: " + skuId));

            Integer currentStock = sku.getStock();
            if (currentStock == null) {
                sku.setStock(quantity);
            } else {
                sku.setStock(currentStock + quantity);
            }
            productSkuRepository.save(sku);
        }
    }

    private String buildSkuSpec(ProductSku sku) {
        List<String> parts = new ArrayList<>();
        if (sku.getSpec1Name() != null && sku.getSpec1Value() != null) {
            parts.add(sku.getSpec1Name() + ": " + sku.getSpec1Value());
        }
        if (sku.getSpec2Name() != null && sku.getSpec2Value() != null) {
            parts.add(sku.getSpec2Name() + ": " + sku.getSpec2Value());
        }
        return String.join(" / ", parts);
    }

    private String resolveMainImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }

        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsMain()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().get(0).getImageUrl());
    }
}
