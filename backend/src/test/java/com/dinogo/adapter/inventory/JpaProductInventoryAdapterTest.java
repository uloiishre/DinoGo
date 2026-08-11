package com.dinogo.adapter.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.entity.Product;
import com.dinogo.entity.ProductImage;
import com.dinogo.entity.ProductSku;
import com.dinogo.port.inventory.OrderSkuSnapshot;
import com.dinogo.repository.ProductSkuRepository;

@ExtendWith(MockitoExtension.class)
class JpaProductInventoryAdapterTest {

    @Mock
    private ProductSkuRepository productSkuRepository;

    private JpaProductInventoryAdapter adapter;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        adapter = new JpaProductInventoryAdapter(productSkuRepository);
        sku = createSku(30, 20, 5, "100.00");
    }

    @Test
    void validateAndDeductReturnsTrustedSnapshotAndDeductsStock() {
        when(productSkuRepository.findByIdForUpdate(30)).thenReturn(Optional.of(sku));

        List<OrderSkuSnapshot> snapshots = adapter.validateAndDeduct(Map.of(30, 2));

        assertThat(sku.getStock()).isEqualTo(3);
        assertThat(snapshots).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.skuId()).isEqualTo(30);
            assertThat(snapshot.productId()).isEqualTo(10);
            assertThat(snapshot.sellerId()).isEqualTo(20);
            assertThat(snapshot.productName()).isEqualTo("Product 30");
            assertThat(snapshot.skuSpec()).isEqualTo("Color: Black / Size: L");
            assertThat(snapshot.productImageUrl()).isEqualTo("https://example.com/30.jpg");
            assertThat(snapshot.unitPrice()).isEqualByComparingTo("100.00");
        });
        verify(productSkuRepository).findByIdForUpdate(30);
    }

    @Test
    void validateAndDeductRejectsMissingSku() {
        when(productSkuRepository.findByIdForUpdate(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.validateAndDeduct(Map.of(999, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SKU does not exist: 999");
    }

    @Test
    void validateAndDeductRejectsInactiveSkuWithoutDeduction() {
        sku.setStatus((byte) 0);
        when(productSkuRepository.findByIdForUpdate(30)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> adapter.validateAndDeduct(Map.of(30, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SKU is not available: 30");

        assertThat(sku.getStock()).isEqualTo(5);
    }

    @Test
    void validateAndDeductValidatesEverySkuBeforeAnyDeduction() {
        ProductSku insufficientSku = createSku(31, 20, 1, "50.00");
        when(productSkuRepository.findByIdForUpdate(30)).thenReturn(Optional.of(sku));
        when(productSkuRepository.findByIdForUpdate(31)).thenReturn(Optional.of(insufficientSku));
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        quantities.put(30, 2);
        quantities.put(31, 2);

        assertThatThrownBy(() -> adapter.validateAndDeduct(quantities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient stock for SKU: 31");

        assertThat(sku.getStock()).isEqualTo(5);
        assertThat(insufficientSku.getStock()).isEqualTo(1);
    }

    @Test
    void validateAndDeductRejectsMultipleSellersWithoutDeduction() {
        ProductSku otherSellerSku = createSku(31, 21, 5, "50.00");
        when(productSkuRepository.findByIdForUpdate(30)).thenReturn(Optional.of(sku));
        when(productSkuRepository.findByIdForUpdate(31)).thenReturn(Optional.of(otherSellerSku));
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        quantities.put(30, 1);
        quantities.put(31, 1);

        assertThatThrownBy(() -> adapter.validateAndDeduct(quantities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An order can only contain products from one seller");

        assertThat(sku.getStock()).isEqualTo(5);
        assertThat(otherSellerSku.getStock()).isEqualTo(5);
    }

    @Test
    void validateAndDeductLocksSkusInAscendingIdOrder() {
        ProductSku otherSku = createSku(31, 20, 5, "50.00");
        when(productSkuRepository.findByIdForUpdate(30)).thenReturn(Optional.of(sku));
        when(productSkuRepository.findByIdForUpdate(31)).thenReturn(Optional.of(otherSku));
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        quantities.put(31, 1);
        quantities.put(30, 1);

        adapter.validateAndDeduct(quantities);

        InOrder lockOrder = inOrder(productSkuRepository);
        lockOrder.verify(productSkuRepository).findByIdForUpdate(30);
        lockOrder.verify(productSkuRepository).findByIdForUpdate(31);
    }

    @Test
    void restoreAddsQuantityBackToEverySku() {
        ProductSku otherSku = createSku(31, 20, 4, "50.00");
        sku.setStock(3);
        when(productSkuRepository.findByIdForUpdate(30)).thenReturn(Optional.of(sku));
        when(productSkuRepository.findByIdForUpdate(31)).thenReturn(Optional.of(otherSku));

        adapter.restore(Map.of(30, 2, 31, 3));

        assertThat(sku.getStock()).isEqualTo(5);
        assertThat(otherSku.getStock()).isEqualTo(7);
    }

    private ProductSku createSku(int skuId, int sellerId, int stock, String price) {
        Product product = new Product();
        product.setProductId(skuId - 20);
        product.setSellerId(sellerId);
        product.setProductName("Product " + skuId);

        ProductImage mainImage = new ProductImage();
        mainImage.setImageUrl("https://example.com/" + skuId + ".jpg");
        mainImage.setIsMain(true);
        product.getImages().add(mainImage);

        ProductSku result = new ProductSku();
        result.setSkuId(skuId);
        result.setProduct(product);
        result.setPrice(new BigDecimal(price));
        result.setStock(stock);
        result.setStatus((byte) 1);
        result.setSpec1Name("Color");
        result.setSpec1Value("Black");
        result.setSpec2Name("Size");
        result.setSpec2Value("L");
        return result;
    }
}
