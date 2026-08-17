package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

        List<Product> findBySeller_SellerIdOrderByProductIdDesc(Integer sellerId);

        Optional<Product> findBySeller_SellerIdAndProductId(
                        Integer sellerId,
                        Integer productId);

        List<Product> findBySubcategorySubcategoryId(Integer subcategoryId);

        List<Product> findByBrandBrandId(Integer brandId);

        List<Product> findBySubcategorySubcategoryIdAndBrandBrandId(
                        Integer subcategoryId,
                        Integer brandId);

        List<Product> findBySubcategoryCategoryCategoryId(Integer categoryId);
}
