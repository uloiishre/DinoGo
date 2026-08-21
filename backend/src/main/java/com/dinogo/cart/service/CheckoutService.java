package com.dinogo.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.dto.CheckoutPreviewItemRequest;
import com.dinogo.cart.dto.CheckoutPreviewRequest;
import com.dinogo.cart.dto.CheckoutPreviewResponse;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import com.dinogo.member.entity.Address;
import com.dinogo.member.repository.AddressRepository;

@Service
@Transactional(readOnly = true)
public class CheckoutService {

        private final ProductSkuRepository productSkuRepository;
        private final AddressRepository addressRepository;
        private final CouponRepository couponRepository;
        private final MemberCouponRepository memberCouponRepository;

        public CheckoutService(
                        ProductSkuRepository productSkuRepository,
                        AddressRepository addressRepository,
                        CouponRepository couponRepository,
                        MemberCouponRepository memberCouponRepository) {

                this.productSkuRepository = productSkuRepository;
                this.addressRepository = addressRepository;
                this.couponRepository = couponRepository;
                this.memberCouponRepository = memberCouponRepository;
        }

        // =========================================================
        // 結帳預覽
        // =========================================================

        public CheckoutPreviewResponse preview(
                        CheckoutPreviewRequest request,
                        Integer memberId) {

                // =====================================================
                // 1. 驗證地址
                // =====================================================

                Address address = addressRepository
                                .findById(request.addressId())
                                .orElseThrow(() -> new RuntimeException(
                                                "收件地址不存在"));

                // 確認地址屬於目前會員
                if (!address.getMember()
                                .getMemberId()
                                .equals(memberId)) {

                        throw new RuntimeException(
                                        "無權使用此收件地址");
                }

                // =====================================================
                // 2. 防止重複 SKU
                // =====================================================

                Set<Integer> skuIds = new HashSet<>();

                BigDecimal subtotal = BigDecimal.ZERO;

                // 用來確認這次結帳是哪一個賣家
                Integer checkoutSellerId = null;

                // =====================================================
                // 3. 檢查每個商品
                // =====================================================

                for (CheckoutPreviewItemRequest item : request.items()) {

                        // -------------------------------------------------
                        // 防止同一 SKU 重複
                        // -------------------------------------------------

                        if (!skuIds.add(item.skuId())) {

                                throw new RuntimeException(
                                                "結帳商品不可有重複 SKU："
                                                                + item.skuId());
                        }

                        // -------------------------------------------------
                        // 數量檢查
                        // -------------------------------------------------

                        if (item.quantity() == null
                                        || item.quantity() <= 0) {

                                throw new RuntimeException(
                                                "商品數量必須大於 0，SKU："
                                                                + item.skuId());
                        }

                        // -------------------------------------------------
                        // 查 SKU
                        // -------------------------------------------------

                        ProductSku sku = productSkuRepository
                                        .findById(item.skuId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "SKU 不存在："
                                                                        + item.skuId()));

                        // -------------------------------------------------
                        // SKU 是否啟用
                        // -------------------------------------------------

                        if (!Byte.valueOf((byte) 1)
                                        .equals(sku.getStatus())) {

                                throw new RuntimeException(
                                                "商品規格已停用，SKU："
                                                                + item.skuId());
                        }

                        // -------------------------------------------------
                        // Product 是否上架
                        // -------------------------------------------------

                        Product product = sku.getProduct();

                        if (!Byte.valueOf((byte) 1)
                                        .equals(product.getStatus())) {

                                throw new RuntimeException(
                                                "商品已下架："
                                                                + product.getProductName());
                        }

                        // -------------------------------------------------
                        // 取得賣家
                        // -------------------------------------------------

                        if (product.getSeller() == null) {

                                throw new RuntimeException(
                                                "商品沒有設定賣家："
                                                                + product.getProductName());
                        }

                        Integer itemSellerId = product.getSeller().getSellerId();

                        // -------------------------------------------------
                        // 確認所有商品都是同一個賣家
                        // -------------------------------------------------

                        if (checkoutSellerId == null) {

                                checkoutSellerId = itemSellerId;

                        } else if (!checkoutSellerId.equals(itemSellerId)) {

                                throw new RuntimeException(
                                                "不同賣家的商品不能一起結帳");
                        }

                        // -------------------------------------------------
                        // 庫存
                        // -------------------------------------------------

                        if (sku.getStock() == null
                                        || item.quantity() > sku.getStock()) {

                                throw new RuntimeException(
                                                product.getProductName()
                                                                + " 庫存不足，目前剩餘 "
                                                                + sku.getStock()
                                                                + " 件");
                        }

                        // -------------------------------------------------
                        // 使用 DB 真實價格
                        // -------------------------------------------------

                        BigDecimal itemSubtotal = sku.getPrice()
                                        .multiply(
                                                        BigDecimal.valueOf(item.quantity()));

                        subtotal = subtotal.add(itemSubtotal);
                }

                // =====================================================
                // 4. 運費
                // =====================================================

                BigDecimal shippingFee = BigDecimal.ZERO;

                // =====================================================
                // 5. 優惠券折扣
                // =====================================================

                BigDecimal discount = BigDecimal.ZERO;
                if (request.memberCouponId() != null) {

                        discount = calculateCouponDiscount(
                                        request.memberCouponId(),
                                        memberId,
                                        checkoutSellerId,
                                        subtotal);
                }

                // =====================================================
                // 6. 總額
                // =====================================================

                BigDecimal totalAmount = subtotal
                                .add(shippingFee)
                                .subtract(discount);

                // 防止折扣超過訂單金額
                if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                        totalAmount = BigDecimal.ZERO;
                }

                // =====================================================
                // 7. 回傳
                // =====================================================

                return new CheckoutPreviewResponse(
                                subtotal,
                                shippingFee,
                                discount,
                                totalAmount);
        }

        // =========================================================
        // 計算優惠券折扣
        // =========================================================

        private BigDecimal calculateCouponDiscount(
                        Integer memberCouponId,
                        Integer memberId,
                        Integer checkoutSellerId,
                        BigDecimal subtotal) {

                // =====================================================
                // 1. 查會員優惠券
                // =====================================================

                MemberCoupon memberCoupon = memberCouponRepository
                                .findById(memberCouponId)
                                .orElseThrow(() -> new RuntimeException(
                                                "會員優惠券不存在"));

                // =====================================================
                // 2. 確認優惠券屬於目前會員
                // =====================================================

                if (memberCoupon.getMemberId() == null
                                || !memberCoupon.getMemberId().equals(memberId)) {

                        throw new RuntimeException(
                                        "您無權使用此優惠券");
                }

                // =====================================================
                // 3. 確認優惠券尚未使用
                // =====================================================

                if (Boolean.TRUE.equals(memberCoupon.getUsed())) {

                        throw new RuntimeException(
                                        "此優惠券已使用");
                }

                // =====================================================
                // 4. 取得真正的 Coupon
                // =====================================================

                Coupon coupon = couponRepository
                                .findById(memberCoupon.getCouponId())
                                .orElseThrow(() -> new RuntimeException(
                                                "優惠券不存在"));

                // =====================================================
                // 5. 優惠券狀態
                // =====================================================

                if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {

                        throw new RuntimeException(
                                        "此優惠券目前無法使用");
                }

                // =====================================================
                // 6. 有效期限
                // =====================================================

                LocalDateTime now = LocalDateTime.now();

                if (coupon.getStartAt() != null
                                && coupon.getStartAt().isAfter(now)) {

                        throw new RuntimeException(
                                        "此優惠券尚未開始");
                }

                if (coupon.getEndAt() != null
                                && coupon.getEndAt().isBefore(now)) {

                        throw new RuntimeException(
                                        "此優惠券已過期");
                }

                // =====================================================
                // Debug
                // =====================================================

                System.out.println("================ COUPON DEBUG ================");
                System.out.println("memberCouponId = " + memberCouponId);
                System.out.println("memberId = " + memberId);
                System.out.println("memberCoupon.memberId = " + memberCoupon.getMemberId());
                System.out.println("memberCoupon.couponId = " + memberCoupon.getCouponId());
                System.out.println("checkoutSellerId = " + checkoutSellerId);
                System.out.println("couponId = " + coupon.getCouponId());
                System.out.println("coupon.sellerId = " + coupon.getSellerId());
                System.out.println("coupon.status = " + coupon.getStatus());
                System.out.println("coupon.discountType = " + coupon.getDiscountType());
                System.out.println("coupon.discountValue = " + coupon.getDiscountValue());
                System.out.println("coupon.minPurchaseAmount = " + coupon.getMinPurchaseAmount());
                System.out.println("memberCoupon.used = " + memberCoupon.getUsed());
                System.out.println("subtotal = " + subtotal);
                System.out.println("===============================================");

                // =====================================================
                // 7. 確認優惠券屬於目前結帳賣家
                // =====================================================

                if (coupon.getSellerId() == null
                                || checkoutSellerId == null
                                || !coupon.getSellerId().equals(checkoutSellerId)) {

                        throw new RuntimeException(
                                        "此優惠券不適用於目前商品");
                }

                // =====================================================
                // 8. 最低消費
                // =====================================================

                if (coupon.getMinPurchaseAmount() != null
                                && subtotal.compareTo(
                                                coupon.getMinPurchaseAmount()) < 0) {

                        throw new RuntimeException(
                                        "此優惠券需滿 NT$ "
                                                        + coupon.getMinPurchaseAmount()
                                                        + " 才能使用");
                }

                // =====================================================
                // 9. 計算折扣
                // =====================================================

                BigDecimal discount = BigDecimal.ZERO;

                String discountType = coupon.getDiscountType();

                // 固定金額
                if ("AMOUNT".equalsIgnoreCase(discountType)
                                || "FIXED".equalsIgnoreCase(discountType)) {

                        discount = coupon.getDiscountValue()
                                        .setScale(0, RoundingMode.DOWN);
                }

                // 百分比
                else if ("PERCENT".equalsIgnoreCase(discountType)
                                || "PERCENTAGE".equalsIgnoreCase(discountType)) {

                        discount = subtotal
                                        .multiply(coupon.getDiscountValue())
                                        .divide(
                                                        BigDecimal.valueOf(100),
                                                        0,
                                                        RoundingMode.DOWN);
                }

                else {

                        throw new RuntimeException(
                                        "不支援的優惠券折扣類型："
                                                        + discountType);
                }

                // =====================================================
                // 10. 折扣不可超過商品小計
                // =====================================================

                if (discount.compareTo(subtotal) > 0) {
                        discount = subtotal;
                }

                return discount;
        }
}
