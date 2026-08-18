package com.dinogo.cart.service;

import java.math.BigDecimal;
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
import com.dinogo.member.entity.Address;
import com.dinogo.member.repository.AddressRepository;

@Service
@Transactional(readOnly = true)
public class CheckoutService {

        private final ProductSkuRepository productSkuRepository;
        private final AddressRepository addressRepository;

        public CheckoutService(
                        ProductSkuRepository productSkuRepository,
                        AddressRepository addressRepository) {

                this.productSkuRepository = productSkuRepository;

                this.addressRepository = addressRepository;
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

                        BigDecimal itemSubtotal = sku.getPrice().multiply(
                                        BigDecimal.valueOf(
                                                        item.quantity()));

                        subtotal = subtotal.add(itemSubtotal);
                }

                // =====================================================
                // 4. 運費
                // =====================================================

                BigDecimal shippingFee = BigDecimal.ZERO;

                // =====================================================
                // 5. 折扣
                // =====================================================

                BigDecimal discount = BigDecimal.ZERO;

                // =====================================================
                // 6. 總額
                // =====================================================

                BigDecimal totalAmount = subtotal
                                .add(shippingFee)
                                .subtract(discount);

                return new CheckoutPreviewResponse(
                                subtotal,
                                shippingFee,
                                discount,
                                totalAmount);
        }
}