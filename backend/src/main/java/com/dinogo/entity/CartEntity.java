package com.dinogo.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="cart", schema = "cart")
public class CartEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="cart_id")
	private int cartId;
//	@OneToOne(optional = false)
//	@JoinColumn(name="member_id")
//	private MemberEntity member;
	@OneToMany(mappedBy="cart")
	private List<CartItemEntity> cartItems;
	public int getCartId() {
		return cartId;
	}
	public List<CartItemEntity> getCartItems() {
		return cartItems;
	}
	public void setCartItems(List<CartItemEntity> cartItems) {
		this.cartItems = cartItems;
	}
//	public MemberEntity getMember() {
//		return member;
//	}
//	public void setMember(MemberEntity member) {
//		this.member = member;
//	}
}