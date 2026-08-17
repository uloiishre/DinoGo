package com.dinogo.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.dto.CartItemRequest;
import com.dinogo.cart.dto.CartItemResponse;
import com.dinogo.cart.dto.CartResponse;
import com.dinogo.cart.dto.SkuOptionResponse;
import com.dinogo.cart.entity.Cart;
import com.dinogo.cart.entity.CartItem;
import com.dinogo.cart.repository.CartItemRepository;
import com.dinogo.cart.repository.CartRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
@Transactional
public class CartService {
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductSkuRepository productSkuRepository;
	private final MemberRepository memberRepository;

	public CartService(ProductSkuRepository productSkuRepository, MemberRepository memberRepository,
			CartRepository cartRepository, CartItemRepository cartItemRepository) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.productSkuRepository = productSkuRepository;
		this.memberRepository = memberRepository;
	}

	// 取得和建立新購物車
	// public Cart getOrCreateCart(Integer memberId) {
	//
	// Member member = memberRepository.findById(memberId)
	// .orElseThrow(() -> new RuntimeException("會員不存在"));
	//
	// return cartRepository.findByMember(member)
	// .orElseGet(() -> {
	// Cart cart = new Cart();
	// cart.setMember(member);
	// return cartRepository.save(cart);
	// });
	// }
	// 取得和建立新購物車
	public Cart getOrCreateCart(String email) {

		// 透過登入者 Email 找會員
		Member member = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("會員不存在"));

		// 找會員的購物車，沒有就建立
		return cartRepository.findByMember(member).orElseGet(() -> {
			Cart cart = new Cart();
			cart.setMember(member);
			return cartRepository.save(cart);
		});
	}

	public CartResponse getCart(String email) {

		Cart cart = getOrCreateCart(email);

		List<CartItemResponse> items = cart.getCartItems()
				.stream()
				.map(this::toResponse)
				.toList();

		return new CartResponse(
				cart.getCartId(),
				items);
	}

	// 新增商品
	public CartItemResponse addItem(String email, Integer skuId, Integer quantity) {

		// 數量檢查
		if (quantity == null || quantity <= 0) {
			throw new RuntimeException("商品數量必須大於 0");
		}

		// 先找到會員的購物車
		Cart cart = getOrCreateCart(email);

		// 找 SKU 是否存在
		ProductSku sku = productSkuRepository.findById(skuId)
				.orElseThrow(() -> new RuntimeException("商品不存在"));

		// 找商品
		Product product = sku.getProduct();

		// 商品是否上架
		if (!Byte.valueOf((byte) 1).equals(product.getStatus())) {
			throw new RuntimeException("商品目前未上架");
		}

		// SKU 是否啟用
		if (!Byte.valueOf((byte) 1).equals(sku.getStatus())) {
			throw new RuntimeException("此商品規格目前未啟用");
		}

		// 用 cartId + skuId 找購物車內是否已有商品
		CartItem item = cartItemRepository
				.findByCartCartIdAndProductSkuSkuId(
						cart.getCartId(),
						skuId);

		// 目前購物車數量
		int currentQuantity = 0;

		if (item != null) {
			currentQuantity = item.getQuantity();
		}

		// 檢查加總後是否超過庫存
		int newQuantity = currentQuantity + quantity;

		if (newQuantity > sku.getStock()) {
			throw new RuntimeException("商品庫存不足");
		}

		// 更新 / 新增
		if (item != null) {

			item.setQuantity(newQuantity);

		} else {

			item = new CartItem();
			item.setCart(cart);
			item.setProductSku(sku);
			item.setQuantity(quantity);
		}

		// 儲存
		item = cartItemRepository.save(item);

		// 統一轉成 CartItemResponse
		return toResponse(item);
	}

	// 修改數量
	// public CartItem updateQuantity(
	// Integer memberId,
	// Integer cartItemId,
	// Integer quantity) {
	// if (quantity <= 0) {
	// throw new RuntimeException("商品數量必須大於 0");
	// }
	// CartItem item = cartItemRepository
	// .findById(cartItemId)
	// .orElseThrow(
	// () -> new RuntimeException("購物車商品不存在"));
	// item.setQuantity(quantity);
	// return cartItemRepository.save(item);
	// }
	// 修改數量
	public CartItemResponse updateQuantity(
			String email,
			Integer cartItemId,
			Integer quantity) {

		if (quantity == null || quantity <= 0) {
			throw new RuntimeException("商品數量必須大於 0");
		}

		// 找登入會員
		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("會員不存在"));

		// 找購物車商品
		CartItem item = cartItemRepository.findById(cartItemId)
				.orElseThrow(() -> new RuntimeException("購物車商品不存在"));

		// 確認商品屬於目前登入會員
		if (!item.getCart()
				.getMember()
				.getMemberId()
				.equals(member.getMemberId())) {

			throw new RuntimeException("無權限修改此購物車商品");
		}

		ProductSku sku = item.getProductSku();
		Product product = sku.getProduct();

		// 商品是否上架
		if (!Byte.valueOf((byte) 1).equals(product.getStatus())) {
			throw new RuntimeException("商品目前未上架");
		}

		// SKU 是否啟用
		if (!Byte.valueOf((byte) 1).equals(sku.getStatus())) {
			throw new RuntimeException("此商品規格目前未啟用");
		}

		// 是否超過庫存
		if (quantity > sku.getStock()) {
			throw new RuntimeException("商品庫存不足");
		}

		// 修改數量
		item.setQuantity(quantity);

		// 儲存
		item = cartItemRepository.save(item);

		// 回傳完整 CartItemResponse
		return toResponse(item);
	}

	// 刪除商品
	// public void deleteItem(Integer memberId, Integer cartItemId) {
	// CartItem item = cartItemRepository.findById(cartItemId).orElseThrow(() -> new
	// RuntimeException("購物車商品不存在"));
	// cartItemRepository.delete(item);
	// }
	// 刪除商品
	public void deleteItem(String email, Integer cartItemId) {

		// 找登入會員
		Member member = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("會員不存在"));

		// 找購物車商品
		CartItem item = cartItemRepository.findById(cartItemId).orElseThrow(() -> new RuntimeException("購物車商品不存在"));

		// 確認商品屬於目前登入會員
		if (!item.getCart().getMember().getMemberId().equals(member.getMemberId())) {
			throw new RuntimeException("無權限刪除此購物車商品");
		}

		cartItemRepository.delete(item);
	}

	// 清空整個購物車
	// public void clearCart(Integer cartId) {
	// List<CartItem> items = cartItemRepository.findByCartCartId(cartId);
	// cartItemRepository.deleteAll(items);
	//
	// }
	public void clearCart(String email) {

		// 找登入會員
		Member member = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("會員不存在"));

		// 找會員的購物車
		Cart cart = cartRepository.findByMember(member).orElseThrow(() -> new RuntimeException("購物車不存在"));

		// 找購物車內所有商品
		List<CartItem> items = cartItemRepository.findByCartCartId(cart.getCartId());

		// 刪除全部商品
		cartItemRepository.deleteAll(items);
	}

	public CartItemResponse updateCartItem(
			Integer cartItemId,
			CartItemRequest request,
			Integer memberId) {

		CartItem cartItem = cartItemRepository
				.findById(cartItemId)
				.orElseThrow(() -> new RuntimeException("找不到購物車商品"));

		// 確認這個 CartItem 是目前登入會員的
		if (!cartItem.getCart()
				.getMember()
				.getMemberId()
				.equals(memberId)) {

			throw new RuntimeException("無權修改此購物車商品");
		}

		// 找新的 SKU
		ProductSku sku = productSkuRepository
				.findById(request.skuId())
				.orElseThrow(() -> new RuntimeException("找不到商品規格"));

		// 確認新的 SKU 屬於同一個商品
		if (!sku.getProduct()
				.getProductId()
				.equals(cartItem.getProductSku()
						.getProduct()
						.getProductId())) {

			throw new RuntimeException("不能更換成其他商品的規格");
		}

		cartItem.setProductSku(sku);
		cartItem.setQuantity(request.quantity());

		cartItemRepository.save(cartItem);

		return toResponse(cartItem);
	}

	private CartItemResponse toResponse(CartItem cartItem) {

		ProductSku sku = cartItem.getProductSku();

		// 取得商品主圖
		String productImage = sku.getProduct()
				.getImages()
				.stream()
				.filter(ProductImage::getIsMain)
				.map(ProductImage::getImageUrl)
				.findFirst()
				.orElse(null);

		// 取得這個商品的所有 SKU
		List<SkuOptionResponse> skus = sku.getProduct()
				.getSkus()
				.stream()
				.map(item -> {

					StringBuilder skuName = new StringBuilder();

					if (item.getSpec1Name() != null
							&& item.getSpec1Value() != null) {

						skuName.append(item.getSpec1Name())
								.append("：")
								.append(item.getSpec1Value());
					}

					if (item.getSpec2Name() != null
							&& item.getSpec2Value() != null) {

						if (!skuName.isEmpty()) {
							skuName.append(" / ");
						}

						skuName.append(item.getSpec2Name())
								.append("：")
								.append(item.getSpec2Value());
					}

					return new SkuOptionResponse(
							item.getSkuId(),
							skuName.toString(),
							item.getPrice());
				})
				.toList();

		return new CartItemResponse(
				cartItem.getCartItemId(),
				sku.getSkuId(),
				sku.getProduct().getProductName(),
				sku.getPrice(),
				cartItem.getQuantity(),
				productImage,
				sku.getProduct().getSeller().getSellerId(),
				sku.getProduct().getSeller().getStoreName(),
				skus);
	}
}
