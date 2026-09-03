package com.dinogo.sysmsg.service.content;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.dinogo.sysmsg.dto.external.OrderInfoResponse;

/** 只負責訂單通知的標題與內容，不負責狀態驗證或儲存。 */
@Component
public class OrderMessageContentFactory {
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
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

    public String title(OrderInfoResponse order, String status, boolean customer) {
        if (customer && "SHIPPED".equals(status)) {
            return "訂單已出貨-" + order.getOrderNo();
        }
        return title(status, customer);
    }

    public String content(OrderInfoResponse order, String status, boolean customer) {
        return switch (status) {
            case "PAID" -> customer
                    ? "訂單 " + order.getOrderNo() + " 已付款成功。"
                    : "收到訂單 " + order.getOrderNo() + "，請開始處理。";
            case "SHIPPED" -> customer ? shippedCustomerContent(order)
                    : "訂單 " + order.getOrderNo() + " 已出貨。";
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

    public String cancelledTitle(OrderInfoResponse order, boolean customer) {
        return "訂單已取消";
    }

    public String cancelledContent(OrderInfoResponse order, boolean customer) {
        if (!customer) return cancelledContent(order);
        if (order.getOrderId() == null || order.getBuyerId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId 或 createdAt，無法產生取消通知");
        }
        String reason = order.getCancelReason() == null ? "未提供原因" : order.getCancelReason();
        return "親愛的會員-" + order.getBuyerId() + "您好:\n"
                + "   感謝您今日光臨！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已取消，\n"
                + "   您的訂單編號為 \"/member/orders/" + order.getOrderId() + "\"，\n"
                + "   取消原因：\n"
                + "       " + reason + "\n"
                + "   歡迎您來信說明，並再次訂購，您的意見是我們最重要的支持！";
    }

    private String shippedCustomerContent(OrderInfoResponse order) {
        if (order.getOrderId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId 或 createdAt，無法產生出貨通知");
        }
        return "親愛的消費者您好:\n"
                + "   您於" + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + "下單之商品已出貨，\n"
                + "   追蹤進度連結：/member/orders/" + order.getOrderId() + "，\n"
                + "   請於貨物送達7日內取貨，感謝您的惠顧!";
    }
}
