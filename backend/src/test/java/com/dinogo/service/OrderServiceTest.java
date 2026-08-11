// package com.dinogo.service;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.doThrow;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.stream.Stream;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.dinogo.sales.dto.order.CreateOrderItemRequest;
// import com.dinogo.sales.dto.order.CreateOrderRequest;
// import com.dinogo.sales.dto.order.CreateOrderResponse;
// import com.dinogo.sales.dto.order.OrderDetailResponse;
// import com.dinogo.sales.dto.order.OrderSummaryResponse;
// import com.dinogo.sales.entity.Order;
// import com.dinogo.sales.entity.OrderItem;
// import com.dinogo.sales.entity.OrderStatus;
// import com.dinogo.exception.ResourceNotFoundException;
// import com.dinogo.port.inventory.OrderSkuSnapshot;
// import com.dinogo.port.inventory.ProductInventoryPort;
// import com.dinogo.port.member.CurrentMemberProvider;
// import com.dinogo.port.coupon.CouponPort;
// import com.dinogo.port.coupon.DiscountResult;
// import com.dinogo.port.cart.CheckoutCartPort;
// import com.dinogo.sales.repository.OrderRepository;

// @ExtendWith(MockitoExtension.class)
// class OrderServiceTest {

//     @Mock
//     private OrderRepository orderRepository;

//     @Mock
//     private ProductInventoryPort productInventoryPort;

//     @Mock
//     private CurrentMemberProvider currentMemberProvider;

//     @Mock
//     private CouponPort couponPort;

//     @Mock
//     private CheckoutCartPort checkoutCartPort;

//     private OrderService orderService;
//     private OrderSkuSnapshot skuSnapshot;

//     @BeforeEach
//     void setUp() {
//         orderService = new OrderService(
//                 orderRepository,
//                 productInventoryPort,
//                 currentMemberProvider,
//                 couponPort,
//                 Optional.of(checkoutCartPort));
//         skuSnapshot = new OrderSkuSnapshot(
//                 30,
//                 10,
//                 20,
//                 "Test product",
//                 "Color: Black / Size: L",
//                 "https://example.com/main.jpg",
//                 new BigDecimal("100.00"));
//     }

//     @Test
//     void createOrderRejectsMissingSku() {
//         when(productInventoryPort.validateAndDeduct(Map.of(999, 1)))
//                 .thenThrow(new IllegalArgumentException("SKU does not exist: 999"));

//         CreateOrderRequest request = createRequest(List.of(new CreateOrderItemRequest(999, 1)));

//         assertThatThrownBy(() -> orderService.createOrder(request))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("SKU does not exist: 999");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderRejectsInactiveSku() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 1)))
//                 .thenThrow(new IllegalArgumentException("SKU is not available: 30"));

//         CreateOrderRequest request = createRequest(List.of(new CreateOrderItemRequest(30, 1)));

//         assertThatThrownBy(() -> orderService.createOrder(request))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("SKU is not available: 30");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderRejectsInsufficientStock() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 6)))
//                 .thenThrow(new IllegalArgumentException("Insufficient stock for SKU: 30"));

//         CreateOrderRequest request = createRequest(List.of(new CreateOrderItemRequest(30, 6)));

//         assertThatThrownBy(() -> orderService.createOrder(request))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Insufficient stock for SKU: 30");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderRejectsProductsFromDifferentSellers() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 1, 31, 1)))
//                 .thenThrow(new IllegalArgumentException(
//                         "An order can only contain products from one seller"));

//         CreateOrderRequest request = createRequest(List.of(
//                 new CreateOrderItemRequest(30, 1),
//                 new CreateOrderItemRequest(31, 1)));

//         assertThatThrownBy(() -> orderService.createOrder(request))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("An order can only contain products from one seller");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderDoesNotDeductEarlierSkuWhenLaterSkuValidationFails() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2, 31, 2)))
//                 .thenThrow(new IllegalArgumentException("Insufficient stock for SKU: 31"));

//         CreateOrderRequest request = createRequest(List.of(
//                 new CreateOrderItemRequest(30, 2),
//                 new CreateOrderItemRequest(31, 2)));

//         assertThatThrownBy(() -> orderService.createOrder(request))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Insufficient stock for SKU: 31");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderRejectsDuplicateSkuWhenCombinedQuantityExceedsStock() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 6)))
//                 .thenThrow(new IllegalArgumentException("Insufficient stock for SKU: 30"));

//         CreateOrderRequest request = createRequest(List.of(
//                 new CreateOrderItemRequest(30, 3),
//                 new CreateOrderItemRequest(30, 3)));

//         assertThatThrownBy(() -> orderService.createOrder(request))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Insufficient stock for SKU: 30");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderMergesDuplicateSkuIntoOneOrderItem() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 5)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, null, 20, new BigDecimal("500.00")))
//                 .thenReturn(new DiscountResult(BigDecimal.ZERO));
//         when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
//             Order order = invocation.getArgument(0);
//             order.setOrderId(1);
//             return order;
//         });

//         CreateOrderRequest request = createRequest(List.of(
//                 new CreateOrderItemRequest(30, 2),
//                 new CreateOrderItemRequest(30, 3)));

//         CreateOrderResponse response = orderService.createOrder(request);

//         ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
//         verify(orderRepository).save(captor.capture());
//         verify(productInventoryPort).validateAndDeduct(Map.of(30, 5));

//         Order savedOrder = captor.getValue();
//         assertThat(savedOrder.getOrderItems()).hasSize(1);
//         OrderItem savedItem = savedOrder.getOrderItems().getFirst();
//         assertThat(savedItem.getQuantity()).isEqualTo(5);
//         assertThat(savedItem.getProductName()).isEqualTo("Test product");
//         assertThat(savedItem.getSkuSpec()).isEqualTo("Color: Black / Size: L");
//         assertThat(savedItem.getProductImageUrl()).isEqualTo("https://example.com/main.jpg");
//         assertThat(savedItem.getUnitPrice()).isEqualByComparingTo("100.00");
//         assertThat(savedItem.getSubtotal()).isEqualByComparingTo("500.00");
//         assertThat(savedOrder.getSubtotalAmount()).isEqualByComparingTo("500.00");
//         assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("500.00");
//         assertThat(savedOrder.getSellerId()).isEqualTo(20);
//         assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
//         assertThat(response.orderNo()).matches("ORD\\d{17}[A-F0-9]{8}");
//         assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
//         verify(couponPort).validateAndReserve(1, null, 20, new BigDecimal("500.00"));
//     }

//     @Test
//     void createOrderRejectsDiscountThatExceedsOrderAmount() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, 5, 20, new BigDecimal("200.00")))
//                 .thenReturn(new DiscountResult(new BigDecimal("300.00")));

//         assertThatThrownBy(() -> orderService.createOrder(
//                 createRequest(5, List.of(new CreateOrderItemRequest(30, 2)))))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Discount amount must not exceed order amount");

//         verify(orderRepository, never()).save(any(Order.class));
//         verify(checkoutCartPort, never()).clearCheckedOutItems(any(), any(), any());
//     }

//     @Test
//     void createOrderAllowsDiscountEqualToOrderAmount() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, 5, 20, new BigDecimal("200.00")))
//                 .thenReturn(new DiscountResult(new BigDecimal("200.00")));
//         when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
//             Order order = invocation.getArgument(0);
//             order.setOrderId(1);
//             return order;
//         });

//         CreateOrderResponse response = orderService.createOrder(
//                 createRequest(5, List.of(new CreateOrderItemRequest(30, 2))));

//         assertThat(response.discountAmount()).isEqualByComparingTo("200.00");
//         assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
//     }

//     @Test
//     void createOrderDoesNotSaveWhenCouponValidationFails() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, 5, 20, new BigDecimal("200.00")))
//                 .thenThrow(new IllegalArgumentException("Coupon is not available: 5"));

//         assertThatThrownBy(() -> orderService.createOrder(
//                 createRequest(5, List.of(new CreateOrderItemRequest(30, 2)))))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Coupon is not available: 5");

//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderDoesNotReserveCouponWhenInventoryValidationFails() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenThrow(new IllegalArgumentException("Insufficient stock for SKU: 30"));

//         assertThatThrownBy(() -> orderService.createOrder(
//                 createRequest(5, List.of(new CreateOrderItemRequest(30, 2)))))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Insufficient stock for SKU: 30");

//         verify(couponPort, never()).validateAndReserve(any(), any(), any(), any());
//         verify(orderRepository, never()).save(any(Order.class));
//     }

//     @Test
//     void createOrderClearsOnlyCheckedOutCartItemsAfterOrderIsSaved() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, null, 20, new BigDecimal("200.00")))
//                 .thenReturn(new DiscountResult(BigDecimal.ZERO));
//         when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
//             Order order = invocation.getArgument(0);
//             order.setOrderId(99);
//             return order;
//         });

//         orderService.createOrder(createRequest(
//                 null,
//                 List.of(101, 102),
//                 List.of(new CreateOrderItemRequest(30, 2))));

//         verify(checkoutCartPort).clearCheckedOutItems(1, List.of(101, 102), 99);
//     }

//     @Test
//     void createOrderDoesNotClearCartWhenOrderSaveFails() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, null, 20, new BigDecimal("200.00")))
//                 .thenReturn(new DiscountResult(BigDecimal.ZERO));
//         when(orderRepository.save(any(Order.class)))
//                 .thenThrow(new IllegalStateException("Order save failed"));

//         assertThatThrownBy(() -> orderService.createOrder(createRequest(
//                 null,
//                 List.of(101, 102),
//                 List.of(new CreateOrderItemRequest(30, 2)))))
//                 .isInstanceOf(IllegalStateException.class)
//                 .hasMessage("Order save failed");

//         verify(checkoutCartPort, never()).clearCheckedOutItems(any(), any(), any());
//     }

//     @Test
//     void createOrderPropagatesCartOwnershipValidationFailure() {
//         when(productInventoryPort.validateAndDeduct(Map.of(30, 2)))
//                 .thenReturn(List.of(skuSnapshot));
//         when(couponPort.validateAndReserve(1, null, 20, new BigDecimal("200.00")))
//                 .thenReturn(new DiscountResult(BigDecimal.ZERO));
//         when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
//             Order order = invocation.getArgument(0);
//             order.setOrderId(99);
//             return order;
//         });
//         doThrow(new IllegalArgumentException("Cart item does not belong to member: 999"))
//                 .when(checkoutCartPort)
//                 .clearCheckedOutItems(1, List.of(999), 99);

//         assertThatThrownBy(() -> orderService.createOrder(createRequest(
//                 null,
//                 List.of(999),
//                 List.of(new CreateOrderItemRequest(30, 2)))))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Cart item does not belong to member: 999");
//     }

//     @Test
//     void getOrderReturnsOrderAndItemSnapshots() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setOrderNo("ORD20260808100000000ABCDEF12");
//         order.setBuyerId(1);
//         order.setSellerId(20);
//         order.setReceiverName("Receiver");
//         order.setReceiverPhone("0912345678");
//         order.setShippingCity("Taipei");
//         order.setShippingDistrict("Zhongzheng");
//         order.setShippingDetailAddress("Test address");
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         order.setSubtotalAmount(new BigDecimal("200.00"));
//         order.setTotalAmount(new BigDecimal("200.00"));

//         OrderItem item = new OrderItem();
//         item.setOrderItemId(100);
//         item.setProductId(10);
//         item.setSkuId(30);
//         item.setProductName("Snapshot product");
//         item.setSkuSpec("Color: Black");
//         item.setProductImageUrl("https://example.com/snapshot.jpg");
//         item.setUnitPrice(new BigDecimal("100.00"));
//         item.setQuantity(2);
//         item.setSubtotal(new BigDecimal("200.00"));
//         item.setIsReviewed(false);
//         order.addOrderItem(item);

//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         OrderDetailResponse response = orderService.getOrder(99);

//         assertThat(response.orderId()).isEqualTo(99);
//         assertThat(response.orderNo()).isEqualTo("ORD20260808100000000ABCDEF12");
//         assertThat(response.items()).hasSize(1);
//         assertThat(response.items().getFirst().productName()).isEqualTo("Snapshot product");
//         assertThat(response.items().getFirst().subtotal()).isEqualByComparingTo("200.00");
//         verify(orderRepository).findById(99);
//     }

//     @Test
//     void getOrderThrowsNotFoundWhenOrderDoesNotExist() {
//         when(orderRepository.findById(999)).thenReturn(Optional.empty());

//         assertThatThrownBy(() -> orderService.getOrder(999))
//                 .isInstanceOf(ResourceNotFoundException.class)
//                 .hasMessage("Order does not exist: 999");

//         verify(orderRepository).findById(999);
//     }

//     @Test
//     void getOrderForMemberReturnsOrderOwnedByAuthenticatedMember() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setBuyerId(1);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         OrderDetailResponse response = orderService.getOrderForMember(99, 1);

//         assertThat(response.orderId()).isEqualTo(99);
//         assertThat(response.buyerId()).isEqualTo(1);
//         verify(orderRepository).findById(99);
//     }

//     @Test
//     void getOrderForMemberHidesOrderOwnedByAnotherMember() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setBuyerId(2);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         assertThatThrownBy(() -> orderService.getOrderForMember(99, 1))
//                 .isInstanceOf(ResourceNotFoundException.class)
//                 .hasMessage("Order does not exist: 99");

//         verify(orderRepository).findById(99);
//     }

//     @Test
//     void getCurrentMemberOrderReturnsOrderOwnedByCurrentMember() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setBuyerId(1);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         when(currentMemberProvider.requireMemberId()).thenReturn(1);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         OrderDetailResponse response = orderService.getCurrentMemberOrder(99);

//         assertThat(response.orderId()).isEqualTo(99);
//         assertThat(response.buyerId()).isEqualTo(1);
//         verify(currentMemberProvider).requireMemberId();
//     }

//     @Test
//     void getCurrentMemberOrderHidesAnotherMembersOrder() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setBuyerId(2);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         when(currentMemberProvider.requireMemberId()).thenReturn(1);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         assertThatThrownBy(() -> orderService.getCurrentMemberOrder(99))
//                 .isInstanceOf(ResourceNotFoundException.class)
//                 .hasMessage("Order does not exist: 99");

//         verify(currentMemberProvider).requireMemberId();
//     }

//     @Test
//     void getMemberOrdersReturnsOrderSummariesWithItems() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setOrderNo("ORD20260808100000000ABCDEF12");
//         order.setSellerId(20);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         order.setTotalAmount(new BigDecimal("200.00"));

//         OrderItem item = new OrderItem();
//         item.setProductId(10);
//         item.setSkuId(30);
//         item.setProductName("Snapshot product");
//         item.setUnitPrice(new BigDecimal("100.00"));
//         item.setQuantity(2);
//         item.setSubtotal(new BigDecimal("200.00"));
//         item.setIsReviewed(false);
//         order.addOrderItem(item);

//         when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(1)).thenReturn(List.of(order));

//         List<OrderSummaryResponse> response = orderService.getMemberOrders(1);

//         assertThat(response).hasSize(1);
//         assertThat(response.getFirst().orderId()).isEqualTo(99);
//         assertThat(response.getFirst().items()).hasSize(1);
//         assertThat(response.getFirst().items().getFirst().productName()).isEqualTo("Snapshot product");
//         verify(orderRepository).findByBuyerIdOrderByCreatedAtDesc(1);
//     }

//     @Test
//     void getMemberOrdersReturnsEmptyListWhenMemberHasNoOrders() {
//         when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(1)).thenReturn(List.of());

//         assertThat(orderService.getMemberOrders(1)).isEmpty();
//         verify(orderRepository).findByBuyerIdOrderByCreatedAtDesc(1);
//     }

//     @Test
//     void getCurrentMemberOrdersUsesTrustedMemberId() {
//         when(currentMemberProvider.requireMemberId()).thenReturn(1);
//         when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(1)).thenReturn(List.of());

//         assertThat(orderService.getCurrentMemberOrders()).isEmpty();

//         verify(currentMemberProvider).requireMemberId();
//         verify(orderRepository).findByBuyerIdOrderByCreatedAtDesc(1);
//     }

//     @ParameterizedTest
//     @MethodSource("validStatusTransitions")
//     void updateOrderStatusAllowsDefinedTransitions(OrderStatus currentStatus, OrderStatus newStatus) {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(currentStatus);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         OrderDetailResponse response = orderService.updateOrderStatus(99, newStatus);

//         assertThat(order.getStatus()).isEqualTo(newStatus);
//         assertThat(response.status()).isEqualTo(newStatus);
//     }

//     @Test
//     void updateOrderStatusRequiresDedicatedCancellationMethod() {
//         assertThatThrownBy(() -> orderService.updateOrderStatus(99, OrderStatus.CANCELLED))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Use cancelOrder to cancel an order");

//         verify(orderRepository, never()).findById(any());
//     }

//     @Test
//     void cancelOrderStoresReasonActorAndTimestamp() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         OrderItem item = new OrderItem();
//         item.setSkuId(30);
//         item.setQuantity(2);
//         order.addOrderItem(item);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         OrderDetailResponse response = orderService.cancelOrder(99, "  Ordered by mistake  ", "BUYER");

//         assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
//         assertThat(order.getCancelReason()).isEqualTo("Ordered by mistake");
//         assertThat(order.getCancelledBy()).isEqualTo("BUYER");
//         assertThat(order.getCancelledAt()).isNotNull();
//         verify(productInventoryPort).restore(Map.of(30, 2));
//         assertThat(response.cancelReason()).isEqualTo("Ordered by mistake");
//         assertThat(response.cancelledBy()).isEqualTo("BUYER");
//         assertThat(response.cancelledAt()).isEqualTo(order.getCancelledAt());
//     }

//     @Test
//     void cancelCurrentMemberOrderCancelsOwnedOrderAsBuyer() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setBuyerId(1);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         when(currentMemberProvider.requireMemberId()).thenReturn(1);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         OrderDetailResponse response = orderService.cancelCurrentMemberOrder(99, "No longer needed");

//         assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
//         assertThat(response.cancelReason()).isEqualTo("No longer needed");
//         assertThat(response.cancelledBy()).isEqualTo("BUYER");
//         verify(currentMemberProvider).requireMemberId();
//     }

//     @Test
//     void cancelCurrentMemberOrderHidesAnotherMembersOrder() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setBuyerId(2);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);
//         when(currentMemberProvider.requireMemberId()).thenReturn(1);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         assertThatThrownBy(() -> orderService.cancelCurrentMemberOrder(99, "No longer needed"))
//                 .isInstanceOf(ResourceNotFoundException.class)
//                 .hasMessage("Order does not exist: 99");

//         verify(productInventoryPort, never()).restore(any());
//         assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
//     }

//     @Test
//     void cancelOrderRestoresStockForEveryOrderItem() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(OrderStatus.PENDING_PAYMENT);

//         OrderItem firstItem = new OrderItem();
//         firstItem.setSkuId(30);
//         firstItem.setQuantity(2);
//         order.addOrderItem(firstItem);

//         OrderItem secondItem = new OrderItem();
//         secondItem.setSkuId(31);
//         secondItem.setQuantity(3);
//         order.addOrderItem(secondItem);

//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         orderService.cancelOrder(99, "No longer needed", "BUYER");

//         verify(productInventoryPort).restore(Map.of(30, 2, 31, 3));
//     }

//     @Test
//     void cancelOrderRejectsRepeatedCancellationWithoutRestoringStockAgain() {
//         LocalDateTime originalCancelledAt = LocalDateTime.of(2026, 8, 8, 12, 0);
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(OrderStatus.CANCELLED);
//         order.setCancelReason("Original reason");
//         order.setCancelledBy("BUYER");
//         order.setCancelledAt(originalCancelledAt);

//         OrderItem item = new OrderItem();
//         item.setSkuId(30);
//         item.setQuantity(2);
//         order.addOrderItem(item);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         assertThatThrownBy(() -> orderService.cancelOrder(99, "Second reason", "SELLER"))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Order cannot be cancelled when status is: CANCELLED");

//         assertThat(order.getCancelReason()).isEqualTo("Original reason");
//         assertThat(order.getCancelledBy()).isEqualTo("BUYER");
//         assertThat(order.getCancelledAt()).isEqualTo(originalCancelledAt);
//         verify(productInventoryPort, never()).restore(any());
//     }

//     @Test
//     void cancelOrderRejectsMissingReasonBeforeLoadingOrder() {
//         assertThatThrownBy(() -> orderService.cancelOrder(99, " ", "BUYER"))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Cancel reason is required");

//         verify(orderRepository, never()).findById(any());
//     }

//     @ParameterizedTest
//     @MethodSource("nonCancellableStatuses")
//     void cancelOrderRejectsCancellationFromNonCancellableStatus(OrderStatus currentStatus) {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(currentStatus);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         assertThatThrownBy(() -> orderService.cancelOrder(99, "No longer needed", "BUYER"))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Order cannot be cancelled when status is: " + currentStatus);

//         assertThat(order.getStatus()).isEqualTo(currentStatus);
//         assertThat(order.getCancelledAt()).isNull();
//     }

//     @Test
//     void updateOrderStatusSetsCompletedTimestamp() {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(OrderStatus.SHIPPED);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         orderService.updateOrderStatus(99, OrderStatus.COMPLETED);

//         assertThat(order.getCompletedAt()).isNotNull();
//     }

//     @ParameterizedTest
//     @MethodSource("invalidStatusTransitions")
//     void updateOrderStatusRejectsUndefinedTransition(OrderStatus currentStatus, OrderStatus newStatus) {
//         Order order = new Order();
//         order.setOrderId(99);
//         order.setStatus(currentStatus);
//         when(orderRepository.findById(99)).thenReturn(Optional.of(order));

//         assertThatThrownBy(() -> orderService.updateOrderStatus(99, newStatus))
//                 .isInstanceOf(IllegalArgumentException.class)
//                 .hasMessage("Invalid order status transition: " + currentStatus + " -> " + newStatus);

//         assertThat(order.getStatus()).isEqualTo(currentStatus);
//         assertThat(order.getCompletedAt()).isNull();
//     }

//     private static Stream<Arguments> validStatusTransitions() {
//         return Stream.of(
//                 Arguments.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
//                 Arguments.of(OrderStatus.PAID, OrderStatus.PROCESSING),
//                 Arguments.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED),
//                 Arguments.of(OrderStatus.SHIPPED, OrderStatus.COMPLETED));
//     }

//     private static Stream<OrderStatus> nonCancellableStatuses() {
//         return Stream.of(
//                 OrderStatus.PAID,
//                 OrderStatus.PROCESSING,
//                 OrderStatus.SHIPPED,
//                 OrderStatus.COMPLETED,
//                 OrderStatus.CANCELLED);
//     }

//     private static Stream<Arguments> invalidStatusTransitions() {
//         return Stream.of(OrderStatus.values())
//                 .flatMap(currentStatus -> Stream.of(OrderStatus.values())
//                         .filter(newStatus -> newStatus != OrderStatus.CANCELLED)
//                         .filter(newStatus -> !isAllowedStatusTransition(currentStatus, newStatus))
//                         .map(newStatus -> Arguments.of(currentStatus, newStatus)));
//     }

//     private static boolean isAllowedStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
//         return (currentStatus == OrderStatus.PENDING_PAYMENT && newStatus == OrderStatus.PAID)
//                 || (currentStatus == OrderStatus.PAID && newStatus == OrderStatus.PROCESSING)
//                 || (currentStatus == OrderStatus.PROCESSING && newStatus == OrderStatus.SHIPPED)
//                 || (currentStatus == OrderStatus.SHIPPED && newStatus == OrderStatus.COMPLETED);
//     }

//     private CreateOrderRequest createRequest(List<CreateOrderItemRequest> items) {
//         return createRequest(null, items);
//     }

//     private CreateOrderRequest createRequest(Integer couponId, List<CreateOrderItemRequest> items) {
//         return createRequest(couponId, null, items);
//     }

//     private CreateOrderRequest createRequest(
//             Integer couponId,
//             List<Integer> cartItemIds,
//             List<CreateOrderItemRequest> items) {
//         return new CreateOrderRequest(
//                 1,
//                 2,
//                 "Receiver",
//                 "0912345678",
//                 "100",
//                 "Taipei",
//                 "Zhongzheng",
//                 "Test address",
//                 null,
//                 couponId,
//                 cartItemIds,
//                 items);
//     }
// }
