// package com.dinogo.msg.entity;

// import java.time.LocalDateTime;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.FetchType;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.PrePersist;
// import jakarta.persistence.Table;

// /**
//  * ============================================================
//  * sysmsg.record
//  * ============================================================
//  *
//  * 訊息發送紀錄 / 收件匣紀錄。
//  *
//  * 一筆 SEND 可以產生多筆 Record。
//  *
//  * 例如：
//  *
//  * OA 廣播：
//  *
//  * SendEntity
//  * send_id = 100
//  * msg_function = OA-001
//  *
//  * ↓
//  *
//  * RecordEntity
//  * record_id = 1
//  * msgto_member_id = 3
//  * msgto_seller_id = NULL
//  *
//  * RecordEntity
//  * record_id = 2
//  * msgto_member_id = NULL
//  * msgto_seller_id = 5
//  *
//  *
//  * ============================================================
//  *
//  * 【Spring Boot 控制】
//  *
//  * 1. 建立 Record 時透過 send_id 取得 msg_function
//  * 2. 建立 Record 時確認 SEND 是否存在
//  * 3. 控制會員 / 商家收件人
//  * 4. 控制一筆 Record 只能有一個收件人類型
//  * 5. 控制 READ / UNREAD 合法轉換
//  * 6. 控制 Record DELETE
//  * 7. 控制 OA Record 刪除時硬刪除
//  * 8. 控制其他 Record 軟刪除
//  * 9. validateRecordOwner()
//  *
//  *
//  * 【SQL Server 控制】
//  *
//  * 1. record_id IDENTITY
//  * 2. INSERT 時 record_created_at
//  * 3. record_status CHECK
//  * 4. msg_function CHECK
//  * 5. send_id FK
//  * 6. 會員收件匣 INDEX
//  * 7. 商家收件匣 INDEX
//  * 8. 發送紀錄 INDEX
//  * 9. msgto_member_id / msgto_seller_id 二選一 CHECK
//  *
//  *
//  * ============================================================
//  */
// @Entity
// @Table(name = "record", schema = "sysmsg")
// public class RecordEntity {

//     /**
//      * ------------------------------------------------------------
//      * record_id
//      * ------------------------------------------------------------
//      *
//      * SQL Server：
//      *
//      * record_id INT IDENTITY(1,1)
//      *
//      * 因此由 SQL Server 產生。
//      */
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "record_id", nullable = false)
//     private Integer recordId;

//     /**
//      * ------------------------------------------------------------
//      * send_id
//      * ------------------------------------------------------------
//      *
//      * FK：
//      *
//      * sysmsg.record.send_id
//      * ↓
//      * sysmsg.send.send_id
//      *
//      * ============================================================
//      *
//      * JPA 關聯：
//      *
//      * RecordEntity
//      * ↓
//      * SendEntity
//      *
//      * ============================================================
//      *
//      * 使用 LAZY：
//      *
//      * 查 Record 收件匣時，
//      * 不需要每次立即把整個 SendEntity 載入。
//      *
//      * 後續如果需要：
//      *
//      * record.getSend()
//      *
//      * 才由 JPA 取得父訊息。
//      *
//      * ============================================================
//      *
//      * 注意：
//      *
//      * 這裡不使用 CascadeType.ALL。
//      *
//      * 因為：
//      *
//      * Record 被刪除
//      *
//      * 不應該連帶刪除：
//      *
//      * sysmsg.send
//      *
//      * 這符合你的規則：
//      *
//      * 「刪除收件匣訊息不影響 sysmsg.send 母樣本」。
//      */
//     @ManyToOne(fetch = FetchType.LAZY, optional = false)
//     @JoinColumn(name = "send_id", nullable = false)
//     private SendEntity send;

//     /**
//      * ------------------------------------------------------------
//      * msg_function
//      * ------------------------------------------------------------
//      *
//      * 例如：
//      *
//      * OA-001
//      * OC-001
//      * OS-001
//      * AC-001
//      * AS-001
//      * SC-001
//      *
//      * ============================================================
//      *
//      * 建立 Record 時：
//      *
//      * send_id
//      * ↓
//      * SendEntity
//      * ↓
//      * getMsgFunction()
//      * ↓
//      * RecordEntity.msgFunction
//      *
//      * ============================================================
//      *
//      * 所以 RecordService.createRecords()
//      * 應該以 SendEntity 的 msgFunction 為準。
//      *
//      * 不應該讓前端自行決定 Record 的 msg_function。
//      */
//     @Column(name = "msg_function", length = 6, nullable = false)
//     private String msgFunction;

//     /**
//      * ------------------------------------------------------------
//      * msgfrom_seller_id
//      * ------------------------------------------------------------
//      *
//      * 訊息發送者 seller_id。
//      *
//      * 來源：
//      *
//      * SendEntity.msgfromSellerId
//      *
//      * 建立 Record 時應由 SendEntity 帶入。
//      *
//      * 不建議由前端自行輸入。
//      */
//     @Column(name = "msgfrom_seller_id", nullable = false)
//     private Integer msgfromSellerId;

//     /**
//      * ------------------------------------------------------------
//      * msgto_member_id
//      * ------------------------------------------------------------
//      *
//      * 會員收件人。
//      *
//      * 可以為 NULL。
//      *
//      * 但 SQL Server CHECK 規定：
//      *
//      * msgto_member_id
//      * 與
//      * msgto_seller_id
//      *
//      * 必須二選一。
//      */
//     @Column(name = "msgto_member_id")
//     private Integer msgtoMemberId;

//     /**
//      * ------------------------------------------------------------
//      * msgto_seller_id
//      * ------------------------------------------------------------
//      *
//      * 商家收件人。
//      *
//      * 可以為 NULL。
//      *
//      * 但 SQL Server CHECK 規定：
//      *
//      * msgto_member_id
//      * 與
//      * msgto_seller_id
//      *
//      * 必須二選一。
//      */
//     @Column(name = "msgto_seller_id")
//     private Integer msgtoSellerId;

//     /**
//      * ------------------------------------------------------------
//      * record_status
//      * ------------------------------------------------------------
//      *
//      * UNREAD = 未讀
//      * READ = 已讀
//      * DELETE = 已刪除
//      *
//      * SQL Server：
//      *
//      * DEFAULT 'UNREAD'
//      *
//      * ============================================================
//      *
//      * 因此建立 Record 時，
//      * Spring Boot 可以不指定狀態，
//      * 讓 SQL Server 使用：
//      *
//      * DEFAULT 'UNREAD'
//      *
//      * ============================================================
//      *
//      * @Enumerated(EnumType.STRING)
//      *
//      *                              讓 Java Enum 儲存：
//      *
//      *                              "UNREAD"
//      *                              "READ"
//      *                              "DELETE"
//      *
//      *                              而不是：
//      *
//      *                              0
//      *                              1
//      *                              2
//      */
//     @Enumerated(EnumType.STRING)
//     @Column(name = "record_status", length = 10, nullable = false)
//     private RecordStatus recordStatus;

//     /**
//      * ------------------------------------------------------------
//      * record_created_at
//      * ------------------------------------------------------------
//      *
//      * SQL Server：
//      *
//      * DEFAULT SYSDATETIME()
//      *
//      * ============================================================
//      *
//      * 這個欄位只代表：
//      *
//      * Record 建立時間
//      *
//      * 不應該在 UPDATE 時改變。
//      *
//      * ============================================================
//      *
//      * 因此：
//      *
//      * 不使用 @PreUpdate。
//      *
//      * 也不在 Service 修改這個欄位。
//      *
//      * ============================================================
//      */
//     @Column(name = "record_created_at", nullable = false, updatable = false)
//     private LocalDateTime recordCreatedAt;

//     // ============================================================
//     // Constructor
//     // ============================================================

//     /**
//      * JPA 必須保留無參數 Constructor。
//      *
//      * 如果使用 Lombok：
//      *
//      * @NoArgsConstructor
//      *
//      *                    可以自動產生。
//      *
//      *                    本專案不使用 Lombok。
//      */
//     protected RecordEntity() {
//     }

//     /**
//      * 建立 Record 使用。
//      *
//      * 注意：
//      *
//      * recordId：
//      * 不傳
//      *
//      * recordCreatedAt：
//      * 不傳
//      *
//      * recordStatus：
//      * 可以不傳
//      *
//      * 因為：
//      *
//      * record_id
//      * → SQL Server IDENTITY
//      *
//      * record_created_at
//      * → SQL Server DEFAULT SYSDATETIME()
//      *
//      * record_status
//      * → SQL Server DEFAULT 'UNREAD'
//      */
//     public RecordEntity(
//             SendEntity send,
//             Integer msgtoMemberId,
//             Integer msgtoSellerId) {

//         this.send = send;

//         /*
//          * msg_function 必須從 SendEntity 取得。
//          *
//          * 不讓呼叫端自己傳入。
//          */
//         if (send != null) {
//             this.msgFunction = send.getMsgFunction();
//             this.msgfromSellerId = send.getMsgfromSellerId();
//         }

//         this.msgtoMemberId = msgtoMemberId;
//         this.msgtoSellerId = msgtoSellerId;
//     }

//     // ============================================================
//     // Entity Lifecycle
//     // ============================================================

//     /**
//      * ------------------------------------------------------------
//      * INSERT 前檢查
//      * ------------------------------------------------------------
//      *
//      * 主要目的：
//      *
//      * Spring Boot 先做必要的基本檢查。
//      *
//      * SQL Server 仍然保留最後的 CHECK 保護。
//      */
//     @PrePersist
//     protected void prePersist() {

//         /*
//          * 每一筆 Record 必須有 Send。
//          */
//         if (this.send == null) {
//             throw new IllegalStateException(
//                     "Record 必須關聯 SendEntity");
//         }

//         /*
//          * msg_function 由 SendEntity 取得。
//          */
//         this.msgFunction = this.send.getMsgFunction();

//         /*
//          * 發送者由 SendEntity 取得。
//          */
//         this.msgfromSellerId = this.send.getMsgfromSellerId();

//         /*
//          * 收件人必須二選一。
//          *
//          * 注意：
//          * 這是 Spring Boot 的第一層保護。
//          *
//          * SQL Server 仍然有 CHECK：
//          *
//          * (member IS NOT NULL AND seller IS NULL)
//          * OR
//          * (member IS NULL AND seller IS NOT NULL)
//          */
//         boolean hasMember = this.msgtoMemberId != null;

//         boolean hasSeller = this.msgtoSellerId != null;

//         if (hasMember == hasSeller) {

//             throw new IllegalStateException(
//                     "Record 收件人必須且只能設定 member_id 或 seller_id 其中一個");
//         }

//         /*
//          * recordStatus 不指定時，
//          * SQL Server DEFAULT 'UNREAD'。
//          *
//          * 如果由 Java 明確設定，
//          * 則必須使用合法 RecordStatus。
//          */
//     }

//     // ============================================================
//     // Business Helper
//     // ============================================================

//     /**
//      * 判斷是否為會員收件匣。
//      */
//     public boolean isMemberInbox() {

//         return this.msgtoMemberId != null
//                 && this.msgtoSellerId == null;
//     }

//     /**
//      * 判斷是否為商家收件匣。
//      */
//     public boolean isSellerInbox() {

//         return this.msgtoMemberId == null
//                 && this.msgtoSellerId != null;
//     }

//     /**
//      * 判斷是否未讀。
//      */
//     public boolean isUnread() {

//         return this.recordStatus == RecordStatus.UNREAD;
//     }

//     /**
//      * 判斷是否已讀。
//      */
//     public boolean isRead() {

//         return this.recordStatus == RecordStatus.READ;
//     }

//     /**
//      * 判斷是否已刪除。
//      */
//     public boolean isDeleted() {

//         return this.recordStatus == RecordStatus.DELETE;
//     }

//     // ============================================================
//     // Getter / Setter
//     // ============================================================

//     /*
//      * 不使用 Lombok。
//      *
//      * 如果使用 Lombok：
//      *
//      * @Getter
//      * 
//      * @Setter
//      *
//      * 可以自動產生以下方法。
//      */

//     public Integer getRecordId() {
//         return recordId;
//     }

//     public void setRecordId(Integer recordId) {
//         this.recordId = recordId;
//     }

//     public SendEntity getSend() {
//         return send;
//     }

//     public void setSend(SendEntity send) {
//         this.send = send;

//         /*
//          * 一旦重新指定 Send，
//          * 同步更新 Record 的來源資料。
//          *
//          * 實際 Service 邏輯仍應避免
//          * 任意修改已存在 Record 的 send。
//          */
//         if (send != null) {
//             this.msgFunction = send.getMsgFunction();
//             this.msgfromSellerId = send.getMsgfromSellerId();
//         }
//     }

//     public String getMsgFunction() {
//         return msgFunction;
//     }

//     public void setMsgFunction(String msgFunction) {
//         /*
//          * 不建議 Service / Controller
//          * 直接修改 msg_function。
//          *
//          * 保留 setter 是為了 JPA / DTO mapping 彈性，
//          * 實際建立 Record 時仍由 SendEntity 決定。
//          */
//         this.msgFunction = msgFunction;
//     }

//     public Integer getMsgfromSellerId() {
//         return msgfromSellerId;
//     }

//     public void setMsgfromSellerId(Integer msgfromSellerId) {
//         this.msgfromSellerId = msgfromSellerId;
//     }

//     public Integer getMsgtoMemberId() {
//         return msgtoMemberId;
//     }

//     public void setMsgtoMemberId(Integer msgtoMemberId) {
//         this.msgtoMemberId = msgtoMemberId;
//     }

//     public Integer getMsgtoSellerId() {
//         return msgtoSellerId;
//     }

//     public void setMsgtoSellerId(Integer msgtoSellerId) {
//         this.msgtoSellerId = msgtoSellerId;
//     }

//     public RecordStatus getRecordStatus() {
//         return recordStatus;
//     }

//     public void setRecordStatus(RecordStatus recordStatus) {
//         this.recordStatus = recordStatus;
//     }

//     public LocalDateTime getRecordCreatedAt() {
//         return recordCreatedAt;
//     }

//     public void setRecordCreatedAt(LocalDateTime recordCreatedAt) {
//         /*
//          * 注意：
//          *
//          * record_created_at 是建立時間。
//          *
//          * SQL Server 負責 INSERT。
//          *
//          * 因此正常業務流程不應該呼叫此 setter
//          * 修改已存在的 Record。
//          */
//         this.recordCreatedAt = recordCreatedAt;
//     }
// }