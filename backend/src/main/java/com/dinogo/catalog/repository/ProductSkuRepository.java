package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.catalog.entity.ProductSku;

import jakarta.persistence.LockModeType;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {
    List<ProductSku> findByProductProductId(Integer productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sku from ProductSku sku where sku.skuId = :skuId")
    Optional<ProductSku> findByIdForUpdate(@Param("skuId") Integer skuId);
}
