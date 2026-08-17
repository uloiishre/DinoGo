// package com.dinogo.msg.entity;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;

// /**
//  * ============================================================
//  * sysmsg.send_disorder
//  * ============================================================
//  *
//  * JOINED：
//  *
//  * SendEntity
//  * ↓
//  * SendDisorderEntity
//  *
//  * 用於取消訂單訊息。
//  *
//  * 對應訂單狀態：
//  *
//  * CANCELLED
//  *
//  * 來源：
//  * order 模組 API
//  *
//  * ============================================================
//  */
// @Entity
// @Table(name = "send_disorder", schema = "sysmsg")
// public class SendDisorderEntity extends SendEntity {

//     @Id
//     @Column(name = "send_disorder_id", nullable = false)
//     private Integer send_id;
//     /**
//      * 訂單 ID。
//      *
//      * 來源：
//      * OrderCancelResponse
//      */
//     @Column(name = "order_id", nullable = false)
//     private Integer orderId;

//     /**
//      * 訂單總金額。
//      */
//     @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
//     private BigDecimal totalAmount;

//     /**
//      * 付款方式 ID。
//      */
//     @Column(name = "payment_method_id", nullable = false)
//     private Integer paymentMethodId;

//     /**
//      * 付款方式名稱。
//      */
//     @Column(name = "method_name", length = 50, nullable = false)
//     private String methodName;

//     /**
//      * 取消原因。
//      *
//      * 由 order 模組提供。
//      */
//     @Column(name = "cancel_reason", length = 500)
//     private String cancelReason;

//     /**
//      * 取消時間。
//      *
//      * 由 order 模組提供。
//      */
//     @Column(name = "cancelled_at", nullable = false)
//     private LocalDateTime cancelledAt;

//     // ============================================================
//     // Constructor
//     // ============================================================

//     /**
//      * Lombok：
//      *
//      * @NoArgsConstructor
//      */
//     protected SendDisorderEntity() {
//         super();
//     }

//     public SendDisorderEntity(
//             Integer msgfromSellerId,
//             String msgFunction,
//             String msgLabel,
//             String sendTitle,
//             String sendContent,
//             SendStatus sendStatus,
//             Integer orderId,
//             BigDecimal totalAmount,
//             Integer paymentMethodId,
//             String methodName,
//             String cancelReason,
//             LocalDateTime cancelledAt) {

//         super(
//                 msgfromSellerId,
//                 msgFunction,
//                 msgLabel,
//                 sendTitle,
//                 sendContent,
//                 sendStatus);

//         this.orderId = orderId;
//         this.totalAmount = totalAmount;
//         this.paymentMethodId = paymentMethodId;
//         this.methodName = methodName;
//         this.cancelReason = cancelReason;
//         this.cancelledAt = cancelledAt;
//     }

//     // ============================================================
//     // Getter / Setter
//     // ============================================================

//     /*
//      * Lombok：
//      *
//      * @Getter
//      * 
//      * @Setter
//      */

//     public Integer getOrderId() {
//         return orderId;
//     }

//     public void setOrderId(Integer orderId) {
//         this.orderId = orderId;
//     }

//     public BigDecimal getTotalAmount() {
//         return totalAmount;
//     }

//     public void setTotalAmount(BigDecimal totalAmount) {
//         this.totalAmount = totalAmount;
//     }

//     public Integer getPaymentMethodId() {
//         return paymentMethodId;
//     }

//     public void setPaymentMethodId(Integer paymentMethodId) {
//         this.paymentMethodId = paymentMethodId;
//     }

//     public String getMethodName() {
//         return methodName;
//     }

//     public void setMethodName(String methodName) {
//         this.methodName = methodName;
//     }

//     public String getCancelReason() {
//         return cancelReason;
//     }

//     public void setCancelReason(String cancelReason) {
//         this.cancelReason = cancelReason;
//     }

//     public LocalDateTime getCancelledAt() {
//         return cancelledAt;
//     }

//     public void setCancelledAt(LocalDateTime cancelledAt) {
//         this.cancelledAt = cancelledAt;
//     }
// }