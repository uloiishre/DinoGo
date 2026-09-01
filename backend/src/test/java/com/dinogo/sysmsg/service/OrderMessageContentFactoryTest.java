package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.service.content.OrderMessageContentFactory;

class OrderMessageContentFactoryTest {
    private final OrderMessageContentFactory factory = new OrderMessageContentFactory();

    @Test
    void createsDifferentCompletedContentForCustomerAndSeller() {
        OrderInfoResponse order = order("ORD-001", new BigDecimal("1200"));

        assertEquals("訂單 ORD-001 已完成。", factory.content(order, "COMPLETED", true));
        assertEquals("訂單 ORD-001 已完成，收入金額為 1200。",
                factory.content(order, "COMPLETED", false));
    }

    @Test
    void rejectsUnsupportedStatus() {
        assertThrows(IllegalStateException.class,
                () -> factory.content(order("ORD-001", BigDecimal.ONE), "UNKNOWN", true));
    }

    private OrderInfoResponse order(String orderNo, BigDecimal totalAmount) {
        OrderInfoResponse order = new OrderInfoResponse();
        order.setOrderNo(orderNo);
        order.setTotalAmount(totalAmount);
        return order;
    }
}
