package com.dinogo.sales.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.CancelOrderRequest;
import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.dto.OrderListResponse;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.service.OrderService;

import jakarta.validation.Valid;

/** 訂單 REST API。 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + response.orderId())).body(response);
    }

    @GetMapping("/member/{buyerId}")
    public ResponseEntity<List<OrderListResponse>> getMemberOrders(
            @PathVariable Integer buyerId) {

        List<OrderListResponse> response = orderService.getMemberOrders(buyerId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getMemberOrder(
            @PathVariable Integer orderId,
            @RequestParam Integer buyerId) {
        OrderDetailResponse response = orderService.getMemberOrder(orderId, buyerId);
        return ResponseEntity.ok(response);
    }

    // @PatchMapping("/{orderId}/cancel")
    // public OrderDetailResponse cancelOrder(
    // @PathVariable Integer orderId,
    // @Valid @RequestBody CancelOrderRequest request) {
    // // 實際仍需要取得登入會員 ID
    // }
}
