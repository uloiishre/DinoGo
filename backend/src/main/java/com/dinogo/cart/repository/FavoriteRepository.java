package com.dinogo.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.cart.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer>{

}
