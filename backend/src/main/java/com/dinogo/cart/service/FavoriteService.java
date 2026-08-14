package com.dinogo.cart.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.dto.AddFavoriteRequest;
import com.dinogo.cart.dto.FavoriteResponse;
import com.dinogo.cart.entity.Favorite;
import com.dinogo.cart.repository.FavoriteRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
@Transactional
public class FavoriteService {
	private final FavoriteRepository favoriteRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;

	public FavoriteService(ProductRepository productRepository, FavoriteRepository favoriteRepository,
			MemberRepository memberRepository) {
		this.favoriteRepository = favoriteRepository;
		this.productRepository = productRepository;
		this.memberRepository = memberRepository;
	}

	// 查詢會員的所有收藏
	@Transactional(readOnly = true)
	public List<FavoriteResponse> getFavorites(Member member) {
		return favoriteRepository.findByMember(member).stream().map(this::toResponse).toList();
	}

	public List<FavoriteResponse> getFavoritesByEmail(String email) {

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		return getFavorites(member);
	}

	// 新增收藏
	public FavoriteResponse addFavorite(Member member, AddFavoriteRequest request) {

		// 真的從資料庫取得商品
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new RuntimeException("商品不存在"));

		// 檢查是否已收藏
		Optional<Favorite> existing = favoriteRepository.findByMemberAndProduct_ProductId(member, request.productId());

		if (existing.isPresent()) {
			return toResponse(existing.get());
		}

		// 建立收藏
		Favorite favorite = new Favorite();
		favorite.setMember(member);
		favorite.setProduct(product);

		Favorite saved = favoriteRepository.save(favorite);

		return toResponse(saved);
	}

	public FavoriteResponse addFavoriteByEmail(
			String email,
			AddFavoriteRequest request) {

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		return addFavorite(member, request);
	}

	// 取消收藏
	public void removeFavorite(Member member, Integer productId) {

		Favorite favorite = favoriteRepository.findByMemberAndProduct_ProductId(member, productId)
				.orElseThrow(() -> new RuntimeException("收藏不存在"));

		favoriteRepository.delete(favorite);
	}

	public void removeFavoriteByEmail(
			String email,
			Integer productId) {

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		removeFavorite(member, productId);
	}

	private FavoriteResponse toResponse(Favorite favorite) {
		Product product = favorite.getProduct();

		String imageUrl = product.getImages()
				.stream()
				.findFirst()
				.map(ProductImage::getImageUrl)
				.orElse(null);

		Integer skuId = product.getSkus()
				.stream()
				.findFirst()
				.map(ProductSku::getSkuId)
				.orElse(null);

		return new FavoriteResponse(
				favorite.getFavoriteId(),
				product.getProductId(),
				skuId,
				product.getProductName(),
				product.getBasePrice(),
				imageUrl);
	}

}
