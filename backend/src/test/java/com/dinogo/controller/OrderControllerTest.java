// package com.dinogo.controller;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import static org.mockito.Mockito.mock;

// import java.math.BigDecimal;
// import java.util.List;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;

// import com.dinogo.sales.dto.order.CreateOrderItemRequest;
// import com.dinogo.sales.dto.order.CreateOrderRequest;
// import com.dinogo.sales.dto.order.CreateOrderResponse;
// import com.dinogo.sales.dto.order.OrderDetailResponse;
// import com.dinogo.sales.dto.order.OrderSummaryResponse;
// import com.dinogo.sales.dto.order.UpdateOrderStatusRequest;
// import com.dinogo.sales.entity.OrderStatus;
// import com.dinogo.sales.service.OrderService;

// @ExtendWith(MockitoExtension.class)
// class OrderControllerTest {

//     @Mock
//     private OrderService orderService;

//     @Test
//     void createOrderReturnsCreatedResponseAndLocation() {
//         CreateOrderRequest request = new CreateOrderRequest(
//                 1,
//                 2,
//                 "Receiver",
//                 "0912345678",
//                 "100",
//                 "Taipei",
//                 "Zhongzheng",
//                 "Test address",
//                 null,
//                 null,
//                 null,
//                 List.of(new CreateOrderItemRequest(30, 2)));
//         CreateOrderResponse serviceResponse = new CreateOrderResponse(
//                 99,
//                 "ORD20260808100000000ABCDEF12",
//                 OrderStatus.PENDING_PAYMENT,
//                 new BigDecimal("200.00"),
//                 BigDecimal.ZERO,
//                 BigDecimal.ZERO,
//                 new BigDecimal("200.00"));
//         when(orderService.createOrder(request)).thenReturn(serviceResponse);

//         OrderController controller = new OrderController(orderService);
//         ResponseEntity<CreateOrderResponse> response = controller.createOrder(request);

//         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//         assertThat(response.getHeaders().getLocation()).hasToString("/api/orders/99");
//         assertThat(response.getBody()).isEqualTo(serviceResponse);
//         verify(orderService).createOrder(request);
//     }

//     @Test
//     void getOrderReturnsOkResponse() {
//         OrderDetailResponse serviceResponse = mock(OrderDetailResponse.class);
//         when(orderService.getOrder(99)).thenReturn(serviceResponse);

//         OrderController controller = new OrderController(orderService);
//         ResponseEntity<OrderDetailResponse> response = controller.getOrder(99);

//         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//         assertThat(response.getBody()).isSameAs(serviceResponse);
//         verify(orderService).getOrder(99);
//     }

//     @Test
//     void getMemberOrdersReturnsOkResponse() {
//         List<OrderSummaryResponse> serviceResponse = List.of(mock(OrderSummaryResponse.class));
//         when(orderService.getMemberOrders(1)).thenReturn(serviceResponse);

//         OrderController controller = new OrderController(orderService);
//         ResponseEntity<List<OrderSummaryResponse>> response = controller.getMemberOrders(1);

//         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//         assertThat(response.getBody()).isSameAs(serviceResponse);
//         verify(orderService).getMemberOrders(1);
//     }

//     @Test
//     void updateOrderStatusReturnsUpdatedOrder() {
//         UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PROCESSING);
//         OrderDetailResponse serviceResponse = mock(OrderDetailResponse.class);
//         when(orderService.updateOrderStatus(99, OrderStatus.PROCESSING)).thenReturn(serviceResponse);

//         OrderController controller = new OrderController(orderService);
//         ResponseEntity<OrderDetailResponse> response = controller.updateOrderStatus(99, request);

//         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//         assertThat(response.getBody()).isSameAs(serviceResponse);
//         verify(orderService).updateOrderStatus(99, OrderStatus.PROCESSING);
//     }
// }
