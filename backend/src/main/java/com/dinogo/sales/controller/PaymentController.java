package com.dinogo.sales.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(
                orderId,
                member.memberId(),
                request);

        return ResponseEntity
                .created(URI.create(
                        "/api/orders/" + orderId
                                + "/payments/" + response.paymentId()))
                .body(response);
    }

}
