package com.dinogo.catalog.service;

import org.springframework.stereotype.Service;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductStatusUpdateRequest;
import com.dinogo.catalog.entity.Brand;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.Subcategory;
import com.dinogo.catalog.repository.BrandRepository;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.catalog.repository.SubcategoryRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

        private final ProductRepository productRepository;
        private final SellerRepository sellerRepository;
        private final SubcategoryRepository subCategoryRepository;
        private final BrandRepository brandRepository;

        public ProductResponse createProduct(ProductCreateRequest request) {

                // 等SellerRepository功能完善
                Seller seller = sellerRepository.findById(1)
                                .orElseThrow(() -> new RuntimeException("找不到賣家"));

                Subcategory subcategory = subCategoryRepository
                                .findById(request.getSubcategoryId())
                                .orElseThrow(() -> new RuntimeException("找不到子分類"));

                Brand brand = brandRepository
                                .findById(request.getBrandId())
                                .orElseThrow(() -> new RuntimeException("找不到品牌"));

                Product product = new Product();

                product.setSeller(seller);
                product.setSubcategory(subcategory);
                product.setBrand(brand);
                product.setProductName(request.getProductName());
                product.setDescription(request.getDescription());
                product.setBasePrice(request.getBasePrice());

                Product savedProduct = productRepository.save(product);

                return new ProductResponse(
                                savedProduct.getProductId(),
                                savedProduct.getSeller().getSellerId(),
                                savedProduct.getSubcategory().getSubcategoryId(),
                                savedProduct.getBrand().getBrandId(),
                                savedProduct.getProductName(),
                                savedProduct.getDescription(),
                                savedProduct.getBasePrice());
        }

        public ProductResponse updateProductStatus(
                        Integer productId,
                        ProductStatusUpdateRequest request) {

                // 暫時，之後從登入資訊取得
                Integer sellerId = 1;

                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                Byte newStatus = request.getStatus();

                if (newStatus == null || (newStatus != 1 && newStatus != 2)) {
                        throw new IllegalArgumentException("商品狀態只能設定為 1（上架）或 2（下架）");
                }

                product.setStatus(newStatus);

                Product savedProduct = productRepository.save(product);

                return new ProductResponse(
                                savedProduct.getProductId(),
                                savedProduct.getSeller().getSellerId(),
                                savedProduct.getSubcategory().getSubcategoryId(),
                                savedProduct.getBrand().getBrandId(),
                                savedProduct.getProductName(),
                                savedProduct.getDescription(),
                                savedProduct.getBasePrice());
        }
}