package com.dinogo.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.service.OrderService;

class OrderControllerTest {

    @Test
    void createOrderReturnsCreatedResponseAndLocation() {
        OrderService orderService = mock(OrderService.class);
        CreateOrderRequest request = new CreateOrderRequest(
                1, 10, BigDecimal.ZERO, null,
                List.of(new CreateOrderItemRequest(100, 1)));
        CreateOrderResponse serviceResponse = new CreateOrderResponse(
                99, "ORD202608110001", OrderStatus.PENDING_PAYMENT,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, null);
        when(orderService.createOrder(request)).thenReturn(serviceResponse);

        ResponseEntity<CreateOrderResponse> response =
                new OrderController(orderService).createOrder(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/orders/99");
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(orderService).createOrder(request);
    }
}
