package com.dinogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.ProductSku;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {
    List<ProductSku> findByProductProductId(Integer productId);
}