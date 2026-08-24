package com.dinogo.sysmsg.dto.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**External跨模組 API 的資料邊界，對應指定的 PAID 等訂單資料
 * ============================================================
 * Order 模組 API
 * ============================================================
 *
 * 假設 Order API：
 *
 * GET /api/orders/{orderId}
 *
 * 例如 Order 模組：
 *
 * http://localhost:8082
 *
 * ============================================================
 *
 * 假設 Order 模組回傳：
 *
 * order_id
 * order_no
 * buyer_id
 * seller_id
 * total_amount
 * payment_method_id
 * method_name
 * created_at
 * status
 *
 * ============================================================
 *
 * sysmsg 不自行維護上述訂單資料。
 *
 * 這個 Response 是：
 *
 * Order API
 *     ↓
 * OrderInfoResponse
 *     ↓
 * OrderMessageService
 *     ↓
 * SendOrderEntity
 */
public class OrderInfoResponse {

    private Integer orderId;

    private String orderNo;

    private Integer buyerId;

    private Integer sellerId;

    private BigDecimal totalAmount;

    private Integer paymentMethodId;

    private String methodName;

    private LocalDateTime createdAt;

    private String status;

    private String cancelReason;

    private LocalDateTime cancelledAt;

    public OrderInfoResponse() {
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Integer buyerId) {
        this.buyerId = buyerId;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
