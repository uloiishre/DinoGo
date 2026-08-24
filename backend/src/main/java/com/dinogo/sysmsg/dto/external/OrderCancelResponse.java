package com.dinogo.sysmsg.dto.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**External跨模組 API 的資料邊界
 * ============================================================
 * Order 模組取消訂單 API
 * ============================================================
 *
 * 假設依賴 DTO。若 order 模組後續提供獨立取消明細 API，
 * 由 GET /api/orders/{orderId}/cancel 接收；目前亦可由 OrderInfoResponse 取得。
 *
 * ============================================================
 *
 * 用於：
 *
 * CANCELLED
 *
 * ↓
 *
 * SendDisorderEntity
 */
public class OrderCancelResponse {

    private Integer orderId;

    private String orderNo;

    private Integer buyerId;

    private Integer sellerId;

    private BigDecimal totalAmount;

    private Integer paymentMethodId;

    private String methodName;

    private String cancelReason;

    private LocalDateTime cancelledAt;

    private String status;

    public OrderCancelResponse() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
