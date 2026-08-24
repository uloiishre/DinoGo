package com.dinogo.catalog.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.dto.ProductImageResponse;
import com.dinogo.catalog.dto.ProductImageSortUpdateRequest;
import com.dinogo.catalog.dto.ProductImageUpdateRequest;
import com.dinogo.catalog.dto.ProductMainImageUpdateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductSkuCreateRequest;
import com.dinogo.catalog.dto.ProductSkuResponse;
import com.dinogo.catalog.dto.ProductSkuUpdateRequest;
import com.dinogo.catalog.dto.ProductUpdateRequest;
import com.dinogo.catalog.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 初始路徑返回所有產品列表
    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer subcategoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String sort) {

        return productService.getProducts(
                keyword,
                categoryId,
                subcategoryId,
                brandId,
                page,
                size,
                sort);
    }

    // 建立商品
    @PostMapping
    public ProductResponse createProduct(@RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    // 上架商品
    @PatchMapping("/{productId}/publish")
    public ProductResponse publishProduct(
            @PathVariable Integer productId) {

        return productService.publishProduct(productId);
    }

    // 下架商品
    @PatchMapping("/{productId}/unpublish")
    public ProductResponse unpublishProduct(
            @PathVariable Integer productId) {

        return productService.unpublishProduct(productId);
    }

    // 商品詳情
    @GetMapping("/{productId}")
    public ProductDetailResponse getProductDetail(
            @PathVariable Integer productId) {

        return productService.getProductDetail(productId);
    }

    // 修改商品詳情
    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
            @PathVariable Integer productId,
            @RequestBody ProductUpdateRequest request) {

        return productService.updateProduct(productId, request);
    }

    // 修改商品SKU
    @PutMapping("/{productId}/skus/{skuId}")
    public ProductSkuResponse updateSku(
            @PathVariable Integer productId,
            @PathVariable Integer skuId,
            @Valid @RequestBody ProductSkuUpdateRequest request) {

        return productService.updateSku(productId, skuId, request);
    }

    @PostMapping("/{productId}/skus/batch")
    public List<ProductSkuResponse> createSkus(
            @PathVariable Integer productId,
            @Valid @RequestBody List<ProductSkuCreateRequest> requests) {

        return productService.createSkus(productId, requests);
    }

    @PatchMapping("/{productId}/skus/{skuId}/disable")
    public ProductSkuResponse disableSku(
            @PathVariable Integer productId,
            @PathVariable Integer skuId) {

        return productService.disableSku(productId, skuId);
    }

    // 修改商品主圖
    @PutMapping("/{productId}/images/main")
    public ProductImageResponse updateMainImage(
            @PathVariable Integer productId,
            @RequestBody ProductMainImageUpdateRequest request) {

        return productService.updateMainImage(
                productId,
                request.getImageId());
    }

    // 修改商品排序
    @PutMapping("/{productId}/images/sort")
    public List<ProductImageResponse> updateImageSort(
            @PathVariable Integer productId,
            @RequestBody List<ProductImageSortUpdateRequest> requests) {

        return productService.updateImageSort(productId, requests);
    }

    // 刪除商品圖片
    @DeleteMapping("/{productId}/images/{imageId}")
    public void deleteImage(
            @PathVariable Integer productId,
            @PathVariable Integer imageId) {

        productService.deleteImage(productId, imageId);
    }

    @PostMapping("/{productId}/images/upload")
    public List<ProductImageResponse> uploadProductImages(
            @PathVariable Integer productId,
            @RequestParam("files") MultipartFile[] files) {

        return productService.uploadProductImages(productId, files);
    }

    // 商品軟刪除
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Integer productId) {

        productService.deleteProduct(productId);

        return ResponseEntity.noContent().build();
    }
}
