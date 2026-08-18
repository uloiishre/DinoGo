package com.dinogo.sales.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.service.OrderService;
import com.dinogo.security.AuthenticatedMember;

@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

    private final OrderService orderService;

    public SellerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getSellerOrder(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ResponseEntity.ok(orderService.getSellerOrder(orderId, member.memberId()));
    }
}
