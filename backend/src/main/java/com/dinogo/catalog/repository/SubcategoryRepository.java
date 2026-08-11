package com.dinogo.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.catalog.entity.Subcategory;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {
}