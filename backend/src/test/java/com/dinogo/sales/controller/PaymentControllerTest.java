package com.dinogo.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.security.AuthenticatedMember;

class PaymentControllerTest {

    @Test
    void createPaymentUsesAuthenticatedMemberAndReturnsLocation() {
        PaymentService paymentService = mock(PaymentService.class);
        AuthenticatedMember member = new AuthenticatedMember(6, "buyer@example.com");
        CreatePaymentRequest request = new CreatePaymentRequest("CREDIT_CARD");
        PaymentResponse serviceResponse = paymentResponse(PaymentStatus.PENDING);
        when(paymentService.createPayment(10, 6, request)).thenReturn(serviceResponse);

        var response = new PaymentController(paymentService)
                .createPayment(10, member, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/orders/10/payments/20");
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(paymentService).createPayment(10, 6, request);
    }

    private PaymentResponse paymentResponse(PaymentStatus status) {
        return new PaymentResponse(
                20,
                "PAY-TEST",
                10,
                "CREDIT_CARD",
                new BigDecimal("1000.00"),
                status,
                null,
                null,
                null,
                null);
    }
}
