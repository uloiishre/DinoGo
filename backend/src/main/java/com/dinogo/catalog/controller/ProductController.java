package com.dinogo.catalog.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductStatusUpdateRequest;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 建立商品
    @PostMapping
    public ProductResponse createProduct(@RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    // update商品status
    @PatchMapping("/{productId}/status")
    public ProductResponse updateProductStatus(
            @PathVariable Integer productId,
            @RequestBody ProductStatusUpdateRequest request) {

        return productService.updateProductStatus(productId, request);
    }
}
