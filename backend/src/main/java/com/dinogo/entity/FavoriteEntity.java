package com.dinogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="favorite", schema = "cart")
public class FavoriteEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="favorite_id")
	private int favoriteId;
//	@ManyToOne
//	@JoinColumn(name="member_id")
//	private MemberEntity member;
//	@ManyToOne
//	@JoinColumn(name="product_id")
//	private ProductEntity product;
//	public void setMember(MemberEntity member) {
//		this.member = member;
//	}
//	public void setProduct(ProductEntity product) {
//		this.product = product;
//	}
	public int getFavoriteId() {
		return favoriteId;
	}
//	public MemberEntity getMember() {
//		return member;
//	}
//	public ProductEntity getProduct() {
//		return product;
//	}
}