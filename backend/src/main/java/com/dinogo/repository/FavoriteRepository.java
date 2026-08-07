package com.dinogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.FavoriteEntity;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Integer>{

}
