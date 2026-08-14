package com.dinogo.msg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * sysmsg.send_order
 * ============================================================
 *
 * JOINED 繼承：
 *
 * SendEntity
 * ↓
 * SendOrderEntity
 *
 * 對應：
 * sysmsg.send_order
 *
 * PK：
 * send_order_id
 *
 * FK：
 * send_order_id → sysmsg.send.send_id
 *
 * JPA JOINED 會讓此 Entity 使用父 Entity 的 sendId。
 */
@Entity
@Table(name = "send_order", schema = "sysmsg")
public class SendOrderEntity extends SendEntity {

    /**
     * ------------------------------------------------------------
     * order_id
     * ------------------------------------------------------------
     *
     * 來源：
     * order 模組 API
     *
     * sysmsg 不自行維護訂單。
     *
     * 假設 order 模組提供：
     *
     * OrderInfoResponse
     *
     * 後續：
     *
     * OrderMessageService
     * ↓
     * OrderInfoResponse
     * ↓
     * SendOrderEntity
     */
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    /**
     * 訂單金額。
     *
     * 來源：
     * order 模組 API
     */
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /**
     * 付款方式 ID。
     *
     * 來源：
     * order 模組 API
     */
    @Column(name = "payment_method_id", nullable = false)
    private Integer paymentMethodId;

    /**
     * 付款方式名稱。
     *
     * 來源：
     * order 模組 API
     */
    @Column(name = "method_name", length = 50, nullable = false)
    private String methodName;

    /**
     * 訂單建立時間。
     *
     * 來源：
     * order 模組 API
     *
     * 不由 sysmsg 自行產生。
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ============================================================
    // Constructor
    // ============================================================

    /**
     * JPA Constructor。
     *
     * Lombok 對應：
     *
     * @NoArgsConstructor
     */
    protected SendOrderEntity() {
        super();
    }

    /**
     * 建立 PAID 等訂單訊息使用。
     *
     * 父 Entity 資料由 super 傳入。
     */
    public SendOrderEntity(
            Integer msgfromSellerId,
            String msgFunction,
            String msgLabel,
            String sendTitle,
            String sendContent,
            SendStatus sendStatus,
            Integer orderId,
            BigDecimal totalAmount,
            Integer paymentMethodId,
            String methodName,
            LocalDateTime createdAt) {

        super(
                msgfromSellerId,
                msgFunction,
                msgLabel,
                sendTitle,
                sendContent,
                sendStatus);

        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.paymentMethodId = paymentMethodId;
        this.methodName = methodName;
        this.createdAt = createdAt;
    }

    // ============================================================
    // Getter / Setter
    // ============================================================

    /*
     * Lombok：
     *
     * @Getter
     * 
     * @Setter
     */

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
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
}