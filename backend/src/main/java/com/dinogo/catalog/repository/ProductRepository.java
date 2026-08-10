package com.dinogo.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}