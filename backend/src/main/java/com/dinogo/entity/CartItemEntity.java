// package com.dinogo.entity;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.Table;

// @Entity
// @Table(name="Cartitem", schema = "cart")
// public class CartItemEntity{
// 	@Id
// 	@GeneratedValue(strategy = GenerationType.IDENTITY)
// 	@Column(name="cart_item_id")
// 	private int cartItemId;	
// 	@ManyToOne
// 	@JoinColumn(name="cart_id")
// 	private CartEntity cart;
// //	@ManyToOne
// //	@JoinColumn(name="sku_id")
// //	private ProductSkuEntity productSku;
// 	@Column(name="quantity")
// 	private int quantity;
// 	public int getQuantity() {
// 		return quantity;
// 	}
// 	public void setCartItemId(int cartItemId) {
// 		this.cartItemId = cartItemId;
// 	}
// //	public void setProductSku(ProductSkuEntity productSku) {
// //		this.productSku = productSku;
// //	}
// 	public void setCart(CartEntity cart) {
// 		this.cart = cart;
// 	}
// 	public void setQuantity(int quantity) {
// 		this.quantity = quantity;
// 	}
// 	public int getCartItemId() {
// 		return cartItemId;
// 	}
// 	public CartEntity getCart() {
// 		return cart;	
// 	}
// //	public ProductSkuEntity getProductSku() {
// //		return productSku;
// //	}
// }