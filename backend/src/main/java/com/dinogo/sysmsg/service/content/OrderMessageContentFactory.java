package com.dinogo.sysmsg.service.content;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.dinogo.sysmsg.dto.external.OrderInfoResponse;

/** 只負責訂單通知的標題與內容，不負責狀態驗證或儲存。 */
@Component
public class OrderMessageContentFactory {
    private static final Map<String, String> CUSTOMER_TITLES = Map.of(
            "PAID", "訂單付款成功",
            "SHIPPED", "訂單已出貨",
            "DELIVERED", "訂單已到貨",
            "COMPLETED", "訂單已完成");
    private static final Map<String, String> SELLER_TITLES = Map.of(
            "PAID", "收到新訂單",
            "SHIPPED", "訂單已出貨",
            "DELIVERED", "訂單已送達",
            "COMPLETED", "訂單完成與收款");

    public String title(String status, boolean customer) {
        return (customer ? CUSTOMER_TITLES : SELLER_TITLES).get(status);
    }

    public String content(OrderInfoResponse order, String status, boolean customer) {
        return switch (status) {
            case "PAID" -> customer
                    ? "訂單 " + order.getOrderNo() + " 已付款成功。"
                    : "收到訂單 " + order.getOrderNo() + "，請開始處理。";
            case "SHIPPED" -> "訂單 " + order.getOrderNo() + " 已出貨。";
            case "DELIVERED" -> "訂單 " + order.getOrderNo() + " 已送達。";
            case "COMPLETED" -> customer
                    ? "訂單 " + order.getOrderNo() + " 已完成。"
                    : "訂單 " + order.getOrderNo() + " 已完成，收入金額為 " + order.getTotalAmount() + "。";
            default -> throw new IllegalStateException("無法產生訂單通知內容：" + status);
        };
    }

    public String cancelledContent(OrderInfoResponse order) {
        String reason = order.getCancelReason() == null ? "未提供原因" : order.getCancelReason();
        return "訂單 " + order.getOrderNo() + " 已取消，原因：" + reason + "。";
    }
}
