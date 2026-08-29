package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer>,
                JpaSpecificationExecutor<Product> {

        List<Product> findBySeller_SellerIdOrderByProductIdDesc(Integer sellerId);

        Optional<Product> findBySeller_SellerIdAndProductId(
                        Integer sellerId,
                        Integer productId);

        List<Product> findBySeller_SellerIdAndStatusNotOrderByProductIdDesc(
                        Integer sellerId,
                        Byte status);

        long countBySeller_SellerIdAndStatus(Integer sellerId, Byte status);

        @Query("""
                        SELECT COALESCE(SUM(p.soldCount), 0)
                        FROM Product p
                        WHERE p.seller.sellerId = :sellerId
                        """)
        long sumSoldCountBySellerId(@Param("sellerId") Integer sellerId);

}
