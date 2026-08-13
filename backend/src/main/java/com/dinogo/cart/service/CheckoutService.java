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

        public CheckoutPreviewResponse preview(
                        CheckoutPreviewRequest request,
                        Integer memberId) {

                // =========================
                // 1. 驗證地址
                // =========================

                Address address = addressRepository
                                .findById(request.addressId())
                                .orElseThrow(() -> new RuntimeException("收件地址不存在"));

                // 確認地址屬於目前登入會員
                if (!address.getMember()
                                .getMemberId()
                                .equals(memberId)) {

                        throw new RuntimeException(
                                        "無權使用此收件地址");
                }

                // =========================
                // 2. 防止同一 SKU 重複
                // =========================

                Set<Integer> skuIds = new HashSet<>();

                BigDecimal subtotal = BigDecimal.ZERO;

                // =========================
                // 3. 查詢商品與計算小計
                // =========================

                for (CheckoutPreviewItemRequest item : request.items()) {

                        if (!skuIds.add(item.skuId())) {

                                throw new RuntimeException(
                                                "結帳商品不可有重複 SKU："
                                                                + item.skuId());
                        }

                        ProductSku sku = productSkuRepository
                                        .findById(item.skuId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "SKU 不存在："
                                                                        + item.skuId()));

                        // =========================
                        // 4. SKU 是否啟用
                        // =========================

                        if (sku.getStatus() == null
                                        || sku.getStatus() != (byte) 1) {

                                throw new RuntimeException(
                                                "SKU 目前未啟用："
                                                                + item.skuId());
                        }

                        // =========================
                        // 5. Product 是否上架
                        // =========================

                        Product product = sku.getProduct();

                        if (product.getStatus() == null
                                        || product.getStatus() != (byte) 1) {

                                throw new RuntimeException(
                                                "商品目前未上架："
                                                                + product.getProductId());
                        }

                        // =========================
                        // 6. 庫存檢查
                        // =========================

                        if (item.quantity() > sku.getStock()) {

                                throw new RuntimeException(
                                                "商品庫存不足，SKU："
                                                                + item.skuId());
                        }

                        // =========================
                        // 7. 使用 DB 真實價格
                        // =========================

                        BigDecimal itemSubtotal = sku.getPrice().multiply(
                                        BigDecimal.valueOf(
                                                        item.quantity()));

                        subtotal = subtotal.add(itemSubtotal);
                }

                // =========================
                // 8. 運費
                // =========================
                //
                // 目前專案尚未有 Shipping Fee 規則/API，
                // 因此不自行假造運費。
                // 等 Shipping API / 運費規則完成後再接。
                //

                BigDecimal shippingFee = BigDecimal.ZERO;

                // =========================
                // 9. 折扣
                // =========================
                //
                // 目前尚未接 Coupon，
                // 因此折扣先為 0。
                //

                BigDecimal discount = BigDecimal.ZERO;

                // =========================
                // 10. 計算總額
                // =========================

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