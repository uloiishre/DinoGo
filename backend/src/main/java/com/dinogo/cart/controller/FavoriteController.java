package com.dinogo.cart.controller;

import org.springframework.web.bind.annotation.RestController;

import com.dinogo.cart.service.FavoriteService;

@RestController
public class FavoriteController {
	private FavoriteService favoriteService;

	public FavoriteController(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	// //取得收藏清單
	// @GetMapping("/api/favorites")
	// //新增
	// @PostMapping("/api/favorites")
	// //取消收藏
	// @DeleteMapping("/api/favorites/{productId}")

}
