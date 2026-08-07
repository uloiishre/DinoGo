package com.dinogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.CartItemEntity;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Integer>{
	List<CartItemEntity> findByCartCartId(Integer cartId);
}
