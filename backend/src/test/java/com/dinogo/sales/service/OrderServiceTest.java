package com.dinogo.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.member.entity.Address;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.entity.Seller;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;
    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, addressRepository, productSkuRepository);
    }

    @Test
    void createOrderBuildsSnapshotsCalculatesAmountsAndDeductsStock() {
        mockOwnedAddress(1, 10);
        ProductSku sku = mockSku(100, 200, 300, "Keyboard", new BigDecimal("500.00"), 5);
        when(productSkuRepository.findById(100)).thenReturn(Optional.of(sku));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(99);
            return order;
        });

        when(productSkuRepository.deductStockIfAvailable(100, 2)).thenReturn(1);
        CreateOrderResponse response = orderService.createOrder(request(
                List.of(new CreateOrderItemRequest(100, 2))), 1);

        assertThat(response.orderId()).isEqualTo(99);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.subtotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.shippingFee()).isEqualByComparingTo("0.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("1000.00");

        org.mockito.Mockito.verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getSellerId()).isEqualTo(300);
        assertThat(savedOrder.getReceiverName()).isEqualTo("王小明");
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getOrderItems().getFirst().getProductName()).isEqualTo("Keyboard");
        assertThat(savedOrder.getPayments()).isEmpty();
    }

    @Test
    void createOrderRejectsAddressOwnedByAnotherMember() {
        mockOwnedAddress(2, 10);

        assertThatThrownBy(() -> orderService.createOrder(request(
                List.of(new CreateOrderItemRequest(100, 1))), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Address does not belong to buyer");
    }

    @Test
    void createOrderRejectsInsufficientStock() {
        mockOwnedAddress(1, 10);
        ProductSku sku = mockSku(100, 200, 300, "Keyboard", new BigDecimal("500.00"), 1);
        when(productSkuRepository.findById(100)).thenReturn(Optional.of(sku));
        when(productSkuRepository.deductStockIfAvailable(100, 2)).thenReturn(0);

        assertThatThrownBy(() -> orderService.createOrder(request(
                List.of(new CreateOrderItemRequest(100, 2))), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient stock for SKU: 100");
    }

    @Test
    void createOrderRejectsProductsFromDifferentSellers() {
        mockOwnedAddress(1, 10);
        ProductSku firstSku = mockSku(100, 200, 300, "Keyboard", BigDecimal.TEN, 5);
        ProductSku secondSku = mockSku(101, 201, 301, "Mouse", BigDecimal.TEN, 5);
        when(productSkuRepository.findById(100))
                .thenReturn(Optional.of(firstSku));
        when(productSkuRepository.findById(101))
                .thenReturn(Optional.of(secondSku));
        when(productSkuRepository.deductStockIfAvailable(100, 1)).thenReturn(1);

        assertThatThrownBy(() -> orderService.createOrder(request(
                List.of(new CreateOrderItemRequest(100, 1), new CreateOrderItemRequest(101, 1))), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("One order can only contain products from one seller");
    }

    @Test
    void cancelOrderUsesLockedQueryAndRestoresStockOnce() {
        Order order = new Order();
        order.setOrderId(99);
        order.setBuyerId(6);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        OrderItem item = new OrderItem();
        item.setSkuId(100);
        item.setQuantity(2);
        order.addOrderItem(item);

        when(orderRepository.findForCancellation(99, 6)).thenReturn(Optional.of(order));
        when(productSkuRepository.restoreStock(100, 2)).thenReturn(1);

        orderService.cancelOrder(99, 6, "buyer cancelled");

        verify(orderRepository).findForCancellation(99, 6);
        verify(orderRepository, never()).findByOrderIdAndBuyerId(99, 6);
        verify(productSkuRepository).restoreStock(100, 2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateStatusRejectsPaidOutsidePaymentFlow() {
        assertThatThrownBy(() -> orderService.updateStatus(99, OrderStatus.PAID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order status PAID can only be set by the payment flow");

        verify(orderRepository, never()).findById(99);
    }

    private CreateOrderRequest request(List<CreateOrderItemRequest> items) {
        return new CreateOrderRequest(10, "請小心包裝", items);
    }

    private void mockOwnedAddress(Integer memberId, Integer addressId) {
        Member member = new Member();
        member.setMemberId(memberId);
        Address address = new Address();
        address.setAddressId(addressId);
        address.setMember(member);
        address.setReceiverName("王小明");
        address.setReceiverPhone("0912345678");
        address.setPostalCode("100");
        address.setCity("台北市");
        address.setDistrict("中正區");
        address.setDetailAddress("測試路 1 號");
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
    }

    private ProductSku mockSku(
            Integer skuId,
            Integer productId,
            Integer sellerId,
            String productName,
            BigDecimal price,
            Integer stock) {
        Seller seller = mock(Seller.class);
        lenient().when(seller.getSellerId()).thenReturn(sellerId);

        Product product = mock(Product.class);
        lenient().when(product.getProductId()).thenReturn(productId);
        lenient().when(product.getProductName()).thenReturn(productName);
        lenient().when(product.getSeller()).thenReturn(seller);
        lenient().when(product.getImages()).thenReturn(new ArrayList<>());

        ProductSku sku = new ProductSku();
        sku.setSkuId(skuId);
        sku.setProduct(product);
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setStatus((byte) 1);
        sku.setSpec1Name("顏色");
        sku.setSpec1Value("黑色");
        return sku;
    }
}
