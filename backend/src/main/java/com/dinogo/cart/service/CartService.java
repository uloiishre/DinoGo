package com.dinogo.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.entity.Cart;
import com.dinogo.cart.entity.CartItem;
import com.dinogo.cart.repository.CartItemRepository;
import com.dinogo.cart.repository.CartRepository;
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
//    public Cart getOrCreateCart(Integer memberId) {
//
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new RuntimeException("會員不存在"));
//
//        return cartRepository.findByMember(member)
//                .orElseGet(() -> {
//                    Cart cart = new Cart();
//                    cart.setMember(member);
//                    return cartRepository.save(cart);
//                });
//    }
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

	// 新增商品
	public CartItem addItem(String email, Integer skuId, Integer quantity) {
		// 先找到會員的購物車
		Cart cart = getOrCreateCart(email);
		// 找 SKU 是否存在
		ProductSku sku = productSkuRepository.findById(skuId).orElseThrow(() -> new RuntimeException("商品不存在"));
		// 用 cartId + skuId 找購物車內是否已有商品
		CartItem item = cartItemRepository.findByCartCartIdAndProductSkuSkuId(cart.getCartId(), skuId);
		if (item != null) {
			// 已存在 → 增加數量
			item.setQuantity(item.getQuantity() + quantity);
		} else {
			// 不存在 → 新增
			item = new CartItem();
			item.setCart(cart);
			item.setProductSku(sku);
			item.setQuantity(quantity);
		}
		return cartItemRepository.save(item);
	}

	// 修改數量
//    public CartItem updateQuantity(
//             Integer memberId,
//            Integer cartItemId,
//            Integer quantity) {
//        if (quantity <= 0) {
//            throw new RuntimeException("商品數量必須大於 0");
//        }
//        CartItem item = cartItemRepository
//                .findById(cartItemId)
//                .orElseThrow(
//                        () -> new RuntimeException("購物車商品不存在"));
//        item.setQuantity(quantity);
//        return cartItemRepository.save(item);
//    }
	// 修改數量
	public CartItem updateQuantity(String email, Integer cartItemId, Integer quantity) {

		if (quantity <= 0) {
			throw new RuntimeException("商品數量必須大於 0");
		}

		// 找登入會員
		Member member = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("會員不存在"));

		// 找購物車商品
		CartItem item = cartItemRepository.findById(cartItemId).orElseThrow(() -> new RuntimeException("購物車商品不存在"));

		// 確認這個商品確實屬於目前登入會員的購物車
		if (!item.getCart().getMember().getMemberId().equals(member.getMemberId())) {
			throw new RuntimeException("無權限修改此購物車商品");
		}

		item.setQuantity(quantity);

		return cartItemRepository.save(item);
	}

	// 刪除商品
//	public void deleteItem(Integer memberId, Integer cartItemId) {
//		CartItem item = cartItemRepository.findById(cartItemId).orElseThrow(() -> new RuntimeException("購物車商品不存在"));
//		cartItemRepository.delete(item);
//	}
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
//	public void clearCart(Integer cartId) {
//		List<CartItem> items = cartItemRepository.findByCartCartId(cartId);
//		cartItemRepository.deleteAll(items);
//
//	}
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
}
