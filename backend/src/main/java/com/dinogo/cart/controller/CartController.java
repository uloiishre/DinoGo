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
	private  final CartService cartService;
	public CartController(CartService cartService) {
		this.cartService = cartService;
	}
	//取得購物車
	@GetMapping("/{memberId}")
	public Cart getCart(@PathVariable Integer memberId) {

        return cartService.getOrCreateCart(memberId);
    }
	
	//新增
	@PostMapping("/{memberId}/items")
	public CartItemResponse addItem(
			@PathVariable Integer memberId,
            @RequestBody CartItemRequest dto) {

		CartItem item =cartService.addItem(
        		memberId,
				dto.skuId(),
				dto.quantity()
		);
		return new CartItemResponse(
	            item.getCartItemId(),
	            item.getProductSku().getSkuId(),
	            item.getQuantity()
	    );
    }
	//修改
	@PutMapping("/items/{cartItemId}")
	public CartItemResponse updateQuantity(
            @PathVariable Integer cartItemId,
            @RequestBody CartItemRequest dto) {

		CartItem item=cartService.updateQuantity(
	            cartItemId,
	            dto.quantity()
	    );

		return new CartItemResponse(
	            item.getCartItemId(),
	            item.getProductSku().getSkuId(),
	            item.getQuantity()
	    );
    }
	//刪除單筆
	@DeleteMapping("/items/{cartItemId}")
	public void deleteItem(
            @PathVariable Integer cartItemId) {

        cartService.deleteItem(cartItemId);
    }
	//清空購物車
	@DeleteMapping("/{cartId}/items")
	public void clearCart(
            @PathVariable Integer cartId) {

        cartService.clearCart(cartId);
    }
}
