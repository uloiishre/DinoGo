package com.dinogo.adapter.inventory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.entity.Product;
import com.dinogo.entity.ProductImage;
import com.dinogo.entity.ProductSku;
import com.dinogo.port.inventory.OrderSkuSnapshot;
import com.dinogo.port.inventory.ProductInventoryPort;
import com.dinogo.repository.ProductSkuRepository;

@Component
public class JpaProductInventoryAdapter implements ProductInventoryPort {

    private final ProductSkuRepository productSkuRepository;

    public JpaProductInventoryAdapter(ProductSkuRepository productSkuRepository) {
        this.productSkuRepository = productSkuRepository;
    }

    @Override
    @Transactional
    public List<OrderSkuSnapshot> validateAndDeduct(Map<Integer, Integer> quantitiesBySku) {
        Map<Integer, ProductSku> skusById = new LinkedHashMap<>();
        Integer sellerId = null;
        List<Integer> sortedSkuIds = new ArrayList<>(quantitiesBySku.keySet());
        sortedSkuIds.sort(Integer::compareTo);

        for (Integer skuId : sortedSkuIds) {
            ProductSku sku = requireSkuForUpdate(skuId);
            validateSku(sku, quantitiesBySku.get(skuId));
            Integer skuSellerId = sku.getProduct().getSellerId();
            if (sellerId == null) {
                sellerId = skuSellerId;
            } else if (!sellerId.equals(skuSellerId)) {
                throw new IllegalArgumentException("An order can only contain products from one seller");
            }
            skusById.put(skuId, sku);
        }

        quantitiesBySku.forEach((skuId, quantity) -> {
            ProductSku sku = skusById.get(skuId);
            sku.setStock(sku.getStock() - quantity);
        });
        return skusById.values().stream().map(this::toSnapshot).toList();
    }

    @Override
    @Transactional
    public void restore(Map<Integer, Integer> quantitiesBySku) {
        List<Integer> sortedSkuIds = new ArrayList<>(quantitiesBySku.keySet());
        sortedSkuIds.sort(Integer::compareTo);
        for (Integer skuId : sortedSkuIds) {
            ProductSku sku = productSkuRepository.findByIdForUpdate(skuId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot restore stock because SKU does not exist: " + skuId));
            if (sku.getStock() == null) {
                throw new IllegalStateException("SKU has no stock value: " + skuId);
            }
            try {
                sku.setStock(Math.addExact(sku.getStock(), quantitiesBySku.get(skuId)));
            } catch (ArithmeticException exception) {
                throw new IllegalStateException(
                        "Stock overflow while restoring SKU: " + skuId, exception);
            }
        }
    }

    private ProductSku requireSkuForUpdate(Integer skuId) {
        return productSkuRepository.findByIdForUpdate(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU does not exist: " + skuId));
    }

    private void validateSku(ProductSku sku, int quantity) {
        if (sku.getStatus() == null || sku.getStatus() != (byte) 1) {
            throw new IllegalArgumentException("SKU is not available: " + sku.getSkuId());
        }
        if (sku.getStock() == null || sku.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for SKU: " + sku.getSkuId());
        }
        if (sku.getPrice() == null || sku.getPrice().signum() < 0) {
            throw new IllegalStateException("SKU has an invalid price: " + sku.getSkuId());
        }
    }

    private OrderSkuSnapshot toSnapshot(ProductSku sku) {
        Product product = sku.getProduct();
        return new OrderSkuSnapshot(sku.getSkuId(), product.getProductId(), product.getSellerId(),
                product.getProductName(), buildSkuSpec(sku), findMainImageUrl(product), sku.getPrice());
    }

    private String buildSkuSpec(ProductSku sku) {
        return Stream.of(formatSpec(sku.getSpec1Name(), sku.getSpec1Value()),
                        formatSpec(sku.getSpec2Name(), sku.getSpec2Value()))
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
    }

    private String formatSpec(String name, String value) {
        if (value == null || value.isBlank()) return null;
        return name == null || name.isBlank() ? value : name + ": " + value;
    }

    private String findMainImageUrl(Product product) {
        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) return null;
        return images.stream().filter(image -> Boolean.TRUE.equals(image.getIsMain()))
                .findFirst().or(() -> images.stream().findFirst())
                .map(ProductImage::getImageUrl).orElse(null);
    }
}
