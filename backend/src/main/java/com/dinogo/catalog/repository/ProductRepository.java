package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dinogo.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer>,
                JpaSpecificationExecutor<Product> {

        List<Product> findBySeller_SellerIdOrderByProductIdDesc(Integer sellerId);

        Optional<Product> findBySeller_SellerIdAndProductId(
                        Integer sellerId,
                        Integer productId);

}
