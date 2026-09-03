package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    void createsCustomerShippedMessageWithOrderDetailLink() {
        OrderInfoResponse order = order("ORD-001", new BigDecimal("1200"));
        order.setOrderId(61);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 14, 30));

        assertEquals("訂單已出貨-ORD-001", factory.title(order, "SHIPPED", true));
        assertEquals("親愛的消費者您好:\n"
                + "   您於2026/09/04 14:30下單之商品已出貨，\n"
                + "   追蹤進度連結：/member/orders/61，\n"
                + "   請於貨物送達7日內取貨，感謝您的惠顧!",
                factory.content(order, "SHIPPED", true));
        assertEquals("訂單已出貨", factory.title(order, "SHIPPED", false));
        assertEquals("訂單 ORD-001 已出貨。", factory.content(order, "SHIPPED", false));
    }

    @Test
    void createsCustomerCancelledMessageWithMemberReasonAndOrderLink() {
        OrderInfoResponse order = order("ORD-002", new BigDecimal("680"));
        order.setOrderId(62);
        order.setBuyerId(15);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 9, 5));
        order.setCancelReason("臨時不需要商品");

        assertEquals("訂單已取消", factory.cancelledTitle(order, true));
        assertEquals("親愛的會員-15您好:\n"
                + "   感謝您今日光臨！您於 2026/09/04 09:05 下單之商品已取消，\n"
                + "   您的訂單編號為 \"/member/orders/62\"，\n"
                + "   取消原因：\n"
                + "       臨時不需要商品\n"
                + "   歡迎您來信說明，並再次訂購，您的意見是我們最重要的支持！",
                factory.cancelledContent(order, true));
        assertEquals("訂單已取消", factory.cancelledTitle(order, false));
        assertEquals("訂單 ORD-002 已取消，原因：臨時不需要商品。",
                factory.cancelledContent(order, false));
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
