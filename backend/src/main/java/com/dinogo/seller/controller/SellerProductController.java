package com.dinogo.seller.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.service.ProductService;
import com.dinogo.seller.dto.SellerProductResponse;
import com.dinogo.seller.service.SellerProductService;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final SellerProductService sellerProductService;
    private final ProductService productService;

    public SellerProductController(
            SellerProductService sellerProductService,
            ProductService productService) {

        this.sellerProductService = sellerProductService;
        this.productService = productService;
    }

    @GetMapping
    public List<SellerProductResponse> getProducts(@RequestParam Integer sellerId) {
        return sellerProductService.getProducts(sellerId);
    }

    @PatchMapping("/{productId}/disable")
    public SellerProductResponse disableProduct(
            @RequestParam Integer sellerId,
            @PathVariable Integer productId) {
        return sellerProductService.disableProduct(sellerId, productId);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getSellerProductDetail(
            @PathVariable Integer productId) {

        return productService.getSellerProductDetail(productId);
    }
}
