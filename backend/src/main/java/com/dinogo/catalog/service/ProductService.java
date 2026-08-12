package com.dinogo.catalog.service;

import org.springframework.stereotype.Service;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.entity.Brand;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.entity.Subcategory;
import com.dinogo.catalog.repository.BrandRepository;
import com.dinogo.catalog.repository.ProductImageRepository;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.catalog.repository.SubcategoryRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

        private final ProductRepository productRepository;
        private final SellerRepository sellerRepository;
        private final SubcategoryRepository subCategoryRepository;
        private final BrandRepository brandRepository;
        private final ProductSkuRepository productSkuRepository;
        private final ProductImageRepository productImageRepository;

        private ProductResponse toProductResponse(
                        Product product,
                        ProductSku sku,
                        ProductImage image) {

                return new ProductResponse(
                                product.getProductId(),
                                product.getSeller().getSellerId(),
                                product.getSubcategory().getSubcategoryId(),
                                product.getBrand().getBrandId(),
                                product.getProductName(),
                                product.getDescription(),
                                product.getBasePrice(),
                                sku.getStock(),
                                image.getImageUrl(),
                                product.getStatus());
        }

        private ProductResponse toProductResponse(Product product) {
                return new ProductResponse(
                                product.getProductId(),
                                product.getSeller().getSellerId(),
                                product.getSubcategory().getSubcategoryId(),
                                product.getBrand().getBrandId(),
                                product.getProductName(),
                                product.getDescription(),
                                product.getBasePrice(),
                                null,
                                null,
                                product.getStatus());
        }

        @Transactional
        public ProductResponse createProduct(ProductCreateRequest request) {

                // 目前先由前端傳 sellerId
                // 未來登入功能完成後，改成從登入身分取得
                Seller seller = sellerRepository
                                .findById(request.getSellerId())
                                .orElseThrow(() -> new RuntimeException("找不到賣家"));

                Subcategory subcategory = subCategoryRepository
                                .findById(request.getSubcategoryId())
                                .orElseThrow(() -> new RuntimeException("找不到子分類"));

                Brand brand = brandRepository
                                .findById(request.getBrandId())
                                .orElseThrow(() -> new RuntimeException("找不到品牌"));

                // =====================
                // 1. 建立 Product
                // =====================

                Product product = new Product();

                product.setSeller(seller);
                product.setSubcategory(subcategory);
                product.setBrand(brand);
                product.setProductName(request.getProductName());
                product.setDescription(request.getDescription());
                product.setBasePrice(request.getBasePrice());

                // 沒傳 status 就預設草稿
                product.setStatus(
                                request.getStatus() != null
                                                ? request.getStatus()
                                                : (byte) 0);

                Product savedProduct = productRepository.save(product);

                // =====================
                // 2. 建立 ProductSku
                // =====================

                ProductSku sku = new ProductSku();

                sku.setProduct(savedProduct);

                // 基本版先建立「無規格 SKU」
                sku.setPrice(request.getBasePrice());
                sku.setStock(request.getStock());
                sku.setStatus((byte) 1);

                ProductSku savedSku = productSkuRepository.save(sku);

                // =====================
                // 3. 建立 ProductImage
                // =====================

                ProductImage image = new ProductImage();

                image.setProduct(savedProduct);
                image.setImageUrl(request.getImageUrl());
                image.setSortOrder(0);
                image.setIsMain(true);

                ProductImage savedImage = productImageRepository.save(image);

                return toProductResponse(savedProduct, savedSku, savedImage);
        }

        public ProductResponse publishProduct(Integer productId) {

                Integer sellerId = 1; // 暫時寫死，之後改成登入賣家

                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                // 草稿 0 或下架 2 才能上架
                if (product.getStatus() != 0 && product.getStatus() != 2) {
                        throw new IllegalArgumentException("目前狀態不可上架");
                }

                product.setStatus((byte) 1);

                Product savedProduct = productRepository.save(product);

                return toProductResponse(savedProduct);
        }

        public ProductResponse unpublishProduct(Integer productId) {

                Integer sellerId = 1; // 暫時寫死，之後改成登入賣家

                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                // 只有上架 1 才能下架
                if (product.getStatus() != 1) {
                        throw new IllegalArgumentException("只有上架中的商品可以下架");
                }

                product.setStatus((byte) 2);

                Product savedProduct = productRepository.save(product);

                return toProductResponse(savedProduct);
        }
}