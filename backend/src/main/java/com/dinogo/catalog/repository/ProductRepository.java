package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findBySellerIdOrderByProductIdDesc(Integer sellerId);

    Optional<Product> findBySellerIdAndProductId(Integer sellerId, Integer productId);
}
