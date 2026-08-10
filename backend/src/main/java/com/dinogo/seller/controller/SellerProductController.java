package com.dinogo.seller.controller;

import com.dinogo.seller.dto.SellerProductResponse;
import com.dinogo.seller.service.SellerProductService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final SellerProductService sellerProductService;

    public SellerProductController(SellerProductService sellerProductService) {
        this.sellerProductService = sellerProductService;
    }

    @GetMapping
    public List<SellerProductResponse> getProducts(@RequestParam Integer sellerId) {
        return sellerProductService.getProducts(sellerId);
    }

    @PatchMapping("/{productId}/disable")
    public SellerProductResponse disableProduct(
            @RequestParam Integer sellerId,
            @PathVariable Integer productId
    ) {
        return sellerProductService.disableProduct(sellerId, productId);
    }
}
