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
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CancelOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.dto.order.OrderDetailResponse;
import com.dinogo.sales.dto.order.OrderSummaryResponse;
import com.dinogo.sales.dto.order.UpdateOrderStatusRequest;
import com.dinogo.sales.service.OrderService;

import jakarta.validation.Valid;

/**
 * 訂單 REST API，負責接收與驗證 HTTP 請求，實際業務規則交由 {@link OrderService} 處理。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 建立訂單，成功時回傳 201，並在 Location header 提供新訂單網址。 */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request);
        URI location = URI.create("/api/orders/" + response.orderId());
        return ResponseEntity.created(location).body(response);
    }

    /** 依訂單 ID 取得完整訂單內容。 */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    /** 取得指定會員的訂單摘要清單，依建立時間由新到舊排序。 */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<OrderSummaryResponse>> getMemberOrders(@PathVariable Integer memberId) {
        return ResponseEntity.ok(orderService.getMemberOrders(memberId));
    }

    /** 取消仍允許取消的訂單，並記錄取消原因與操作者。 */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(
                orderId,
                request.cancelReason(),
                request.cancelledBy()));
    }

    /** 依訂單狀態機更新狀態；取消操作必須使用獨立的 cancel API。 */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(
            @PathVariable Integer orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.status()));
    }
}
