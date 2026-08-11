package com.dinogo.cart.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.cart.dto.CartItemRequest;
import com.dinogo.cart.dto.CartItemResponse;
import com.dinogo.cart.entity.Cart;
import com.dinogo.cart.entity.CartItem;
import com.dinogo.cart.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {
	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	// 取得購物車
	@GetMapping("/{memberId}")
	public Cart getCart(@PathVariable Integer memberId) {

		return cartService.getOrCreateCart(memberId);
	}
	//帳號
//	@GetMapping
//	public Cart getCart(Authentication authentication) {
//
//	    Integer memberId = // 從目前登入者取得
//
//	    return cartService.getOrCreateCart(memberId);
//	}
	// 新增
	@PostMapping("/{memberId}/items")
	public CartItemResponse addItem(
			@PathVariable Integer memberId,
			@RequestBody CartItemRequest dto) {

		CartItem item = cartService.addItem(
				memberId,
				dto.skuId(),
				dto.quantity());
		return new CartItemResponse(
				item.getCartItemId(),
				item.getProductSku().getSkuId(),
				item.getQuantity());
	}
	//帳號
//	@PostMapping("/items")
//	public CartItemResponse addItem(
//	        Authentication authentication,
//	        @RequestBody CartItemRequest dto) {
//
//	    Integer memberId = // 從 JWT 取得;
//
//	    CartItem item = cartService.addItem(
//	            memberId,
//	            dto.skuId(),
//	            dto.quantity());
//
//	    return new CartItemResponse(
//	            item.getCartItemId(),
//	            item.getProductSku().getSkuId(),
//	            item.getQuantity());
//	}
	// 修改
	@PutMapping("/items/{cartItemId}")
	public CartItemResponse updateQuantity(
			@PathVariable Integer cartItemId,
			@RequestBody CartItemRequest dto) {

		CartItem item = cartService.updateQuantity(
				cartItemId,
				dto.quantity());

		return new CartItemResponse(
				item.getCartItemId(),
				item.getProductSku().getSkuId(),
				item.getQuantity());
	}
	//帳號
//	 @PutMapping("/items/{cartItemId}")
//	    public CartItemResponse updateQuantity(
//	            Authentication authentication,
//	            @PathVariable Integer cartItemId,
//	            @RequestBody CartItemRequest dto) {
//
//	        Integer memberId = getMemberId(authentication);
//
//	        CartItem item = cartService.updateQuantity(
//	                memberId,
//	                cartItemId,
//	                dto.quantity());
//
//	        return new CartItemResponse(
//	                item.getCartItemId(),
//	                item.getProductSku().getSkuId(),
//	                item.getQuantity());
//	    }
	// 刪除單筆
	@DeleteMapping("/items/{cartItemId}")
	public void deleteItem(
			@PathVariable Integer cartItemId) {

		cartService.deleteItem(cartItemId);
	}
	//帳號
//	@DeleteMapping("/items/{cartItemId}")
//    public void deleteItem(
//            Authentication authentication,
//            @PathVariable Integer cartItemId) {
//
//        Integer memberId = getMemberId(authentication);
//
//        cartService.deleteItem(memberId, cartItemId);
//    }
	// 清空購物車
	@DeleteMapping("/{cartId}/items")
	public void clearCart(
			@PathVariable Integer cartId) {

		cartService.clearCart(cartId);
	}
	//帳號
//	@DeleteMapping("/items")
//    public void clearCart(Authentication authentication) {
//
//        Integer memberId = getMemberId(authentication);
//
//        cartService.clearCart(memberId);
//    }
//
//    private Integer getMemberId(Authentication authentication) {
//        // JWT 完成後從 Authentication 取得
//        return 1; // 暫時測試
//    }
}
