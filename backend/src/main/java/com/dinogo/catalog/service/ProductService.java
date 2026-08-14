package com.dinogo.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.dto.ProductImageCreateRequest;
import com.dinogo.catalog.dto.ProductImageResponse;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductSkuCreateRequest;
import com.dinogo.catalog.dto.ProductSkuResponse;
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

        private ProductResponse toProductResponse(Product product) {

                String imageUrl = product.getImages()
                                .stream()
                                .filter(ProductImage::getIsMain)
                                .findFirst()
                                .map(ProductImage::getImageUrl)
                                .orElse(null);

                Integer stock = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .mapToInt(ProductSku::getStock)
                                .sum();

                return new ProductResponse(
                                product.getProductId(),
                                product.getSeller().getSellerId(),
                                product.getSubcategory().getSubcategoryId(),
                                product.getBrand().getBrandId(),
                                product.getProductName(),
                                product.getDescription(),
                                product.getBasePrice(),
                                stock,
                                imageUrl,
                                product.getStatus());
        }

        // 建立商品
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

                if (request.getSkus() != null) {
                        for (ProductSkuCreateRequest skuCreateRequest : request.getSkus()) {

                                ProductSku sku = ProductSku.builder()
                                                .product(product)
                                                .spec1Name(skuCreateRequest.getSpec1Name())
                                                .spec1Value(skuCreateRequest.getSpec1Value())
                                                .spec2Name(skuCreateRequest.getSpec2Name())
                                                .spec2Value(skuCreateRequest.getSpec2Value())
                                                .price(skuCreateRequest.getPrice())
                                                .stock(skuCreateRequest.getStock())
                                                .status((byte) 1)
                                                .build();

                                ProductSku savedSku = productSkuRepository.save(sku);
                                savedProduct.getSkus().add(savedSku);
                        }
                }

                // =====================
                // 3. 建立 ProductImage
                // =====================
                if (request.getImages() != null) {
                        for (int i = 0; i < request.getImages().size(); i++) {

                                ProductImageCreateRequest imageRequest = request.getImages().get(i);

                                ProductImage image = ProductImage.builder()
                                                .product(product)
                                                .imageUrl(imageRequest.getImageUrl())
                                                .sortOrder(imageRequest.getSortOrder())
                                                .isMain(i == 0)
                                                .build();

                                ProductImage savedImg = productImageRepository.save(image);
                                savedProduct.getImages().add(savedImg);
                        }
                }

                System.out.println("===== RESPONSE DEBUG =====");

                System.out.println("SKU 數量 = " + savedProduct.getSkus().size());

                savedProduct.getSkus().forEach(sku -> {
                        System.out.println(
                                        "skuId = " + sku.getSkuId()
                                                        + ", stock = " + sku.getStock()
                                                        + ", status = " + sku.getStatus());
                });

                System.out.println("圖片數量 = " + savedProduct.getImages().size());

                savedProduct.getImages().forEach(image -> {
                        System.out.println(
                                        "imageId = " + image.getImageId()
                                                        + ", imageUrl = " + image.getImageUrl()
                                                        + ", isMain = " + image.getIsMain());
                });

                return toProductResponse(savedProduct);
        }

        // 上架商品
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

        // 下架商品
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

        // 讀取商品列表
        public List<ProductResponse> getProducts() {
                return productRepository.findAll()
                                .stream()
                                .map(this::toProductResponse)
                                .toList();
        }

        // 讀取商品詳情
        public ProductDetailResponse getProductDetail(Integer productId) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品"));

                List<ProductImageResponse> images = product.getImages()
                                .stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .build())
                                .toList();

                List<ProductSkuResponse> skus = product.getSkus()
                                .stream()
                                .map(sku -> ProductSkuResponse.builder()
                                                .skuId(sku.getSkuId())
                                                .spec1Name(sku.getSpec1Name())
                                                .spec1Value(sku.getSpec1Value())
                                                .spec2Name(sku.getSpec2Name())
                                                .spec2Value(sku.getSpec2Value())
                                                .price(sku.getPrice())
                                                .stock(sku.getStock())
                                                .status(sku.getStatus())
                                                .build())
                                .toList();

                return ProductDetailResponse.builder()
                                .productId(product.getProductId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .basePrice(product.getBasePrice())
                                .status(product.getStatus())
                                .viewCount(product.getViewCount())
                                .soldCount(product.getSoldCount())

                                .brandId(product.getBrand().getBrandId())
                                .brandName(product.getBrand().getBrandName())

                                .subcategoryId(product.getSubcategory().getSubcategoryId())
                                .subcategoryName(product.getSubcategory().getSubcategoryName())

                                .categoryId(product.getSubcategory().getCategory().getCategoryId())
                                .categoryName(product.getSubcategory().getCategory().getCategoryName())

                                .images(images)
                                .skus(skus)

                                .build();
        }
}