package com.dinogo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.exception.GlobalExceptionHandler;
import com.dinogo.exception.ResourceNotFoundException;
import com.dinogo.sales.controller.OrderController;
import com.dinogo.sales.dto.order.OrderDetailResponse;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.service.OrderService;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void cancelOrderReturnsOkWithCancellationDetails() throws Exception {
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 8, 8, 15, 0);
        when(orderService.cancelOrder(99, "Changed my mind", "BUYER"))
                .thenReturn(orderResponse(
                        OrderStatus.CANCELLED,
                        "Changed my mind",
                        "BUYER",
                        cancelledAt));

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancelReason": "Changed my mind",
                                  "cancelledBy": "BUYER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(99))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Changed my mind"))
                .andExpect(jsonPath("$.cancelledBy").value("BUYER"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-08-08T15:00:00"));
    }

    @Test
    void updateOrderStatusReturnsOkWithUpdatedStatus() throws Exception {
        when(orderService.updateOrderStatus(99, OrderStatus.PAID))
                .thenReturn(orderResponse(OrderStatus.PAID, null, null, null));

        mockMvc.perform(patch("/api/orders/{orderId}/status", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PAID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(99))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getOrderReturnsNotFoundErrorResponse() throws Exception {
        when(orderService.getOrder(999))
                .thenThrow(new ResourceNotFoundException("Order does not exist: 999"));

        mockMvc.perform(get("/api/orders/{orderId}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order does not exist: 999"));
    }

    @Test
    void cancelOrderRejectsBlankReason() throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/cancel", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancelReason": " ",
                                  "cancelledBy": "BUYER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.cancelReason").value("Cancel reason is required"));
    }

    @Test
    void cancelOrderRejectsInvalidActor() throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/cancel", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancelReason": "Changed my mind",
                                  "cancelledBy": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cancelledBy")
                        .value("Cancelled by must be BUYER, SELLER, or SYSTEM"));
    }

    @Test
    void cancelOrderRejectsRepeatedCancellation() throws Exception {
        when(orderService.cancelOrder(99, "Changed my mind", "BUYER"))
                .thenThrow(new IllegalArgumentException(
                        "Order cannot be cancelled when status is: CANCELLED"));

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancelReason": "Changed my mind",
                                  "cancelledBy": "BUYER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Order cannot be cancelled when status is: CANCELLED"));
    }

    @Test
    void updateOrderStatusRejectsMissingStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/status", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.status").value("Order status is required"));
    }

    @Test
    void updateOrderStatusRejectsUnknownEnumValue() throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/status", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is invalid"));
    }

    @Test
    void updateOrderStatusRejectsInvalidTransition() throws Exception {
        when(orderService.updateOrderStatus(99, OrderStatus.SHIPPED))
                .thenThrow(new IllegalArgumentException(
                        "Invalid order status transition: PENDING_PAYMENT -> SHIPPED"));

        mockMvc.perform(patch("/api/orders/{orderId}/status", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SHIPPED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid order status transition: PENDING_PAYMENT -> SHIPPED"));
    }

    private OrderDetailResponse orderResponse(
            OrderStatus status,
            String cancelReason,
            String cancelledBy,
            LocalDateTime cancelledAt) {
        return new OrderDetailResponse(
                99,
                "ORD20260808100000000ABCDEF12",
                1,
                20,
                2,
                "Receiver",
                "0912345678",
                "100",
                "Taipei",
                "Zhongzheng",
                "Test address",
                status,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("200.00"),
                null,
                cancelReason,
                cancelledBy,
                cancelledAt,
                null,
                LocalDateTime.of(2026, 8, 8, 10, 0),
                LocalDateTime.of(2026, 8, 8, 15, 0),
                List.of());
    }
}
