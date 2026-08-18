package com.dinogo.catalog.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.service.ProductService;

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
            @RequestParam(defaultValue = "12") Integer size) {

        return productService.getProducts(
                keyword,
                categoryId,
                subcategoryId,
                brandId,
                page,
                size);
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
}
