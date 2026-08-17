// package com.dinogo.msg.entity;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.Inheritance;
// import jakarta.persistence.InheritanceType;
// import jakarta.persistence.PrePersist;
// import jakarta.persistence.PreUpdate;
// import jakarta.persistence.Table;

// import org.hibernate.annotations.DynamicInsert;
// import org.hibernate.annotations.Generated;
// import org.hibernate.generator.EventType;

// import java.time.LocalDateTime;

// /**
//  * ============================================================
//  * sysmsg.send
//  * ============================================================
//  *
//  * 父 Entity。
//  *
//  * 對應：
//  * sysmsg.send
//  *
//  * JPA 繼承：
//  * SendEntity
//  * ├── SendOrderEntity
//  * ├── SendDisorderEntity
//  * └── SendSellerEntity
//  *
//  * 使用 JOINED：
//  * 父表保存共同欄位
//  * 子表保存各自特殊欄位
//  *
//  * ============================================================
//  *
//  * 【Spring Boot 控制】
//  * 1. msg_label 沒有內容時，自動使用 send_title
//  * 2. send_upd_at 更新時由 Spring Boot 控制
//  * 3. 一個 send_id 只能建立一種子 Entity
//  * 4. msg_function 自動取號由 Service + Transaction + 悲觀鎖處理
//  * 5. SAVE / SEND / DELETE 合法狀態轉換由 Service 控制
//  *
//  * 【SQL Server 控制】
//  * 1. send_id IDENTITY
//  * 2. INSERT 時 send_upd_at DEFAULT SYSDATETIME()
//  * 3. send_status CHECK
//  * 4. msg_function CHECK
//  * 5. msg_label / send_title / send_content CHECK
//  * 6. SAVE 的 msg_function UNIQUE FILTERED INDEX
//  *
//  * 【重要】
//  * SQL Server 的 send_upd_at INSERT DEFAULT 必須讓 DB 有機會執行。
//  *
//  * @DynamicInsert 讓 Hibernate INSERT 時，
//  *                對尚未指定的欄位不強制送 NULL，
//  *                因此 SQL Server 才能使用 DEFAULT SYSDATETIME()。
//  */
// @Entity
// @Table(name = "send", schema = "sysmsg")
// @Inheritance(strategy = InheritanceType.JOINED)
// @DynamicInsert
// public class SendEntity {

//     /**
//      * ------------------------------------------------------------
//      * send_id
//      * ------------------------------------------------------------
//      *
//      * SQL Server：
//      *
//      * send_id INT IDENTITY(1,1)
//      *
//      * 由 SQL Server 產生。
//      *
//      * Spring Boot 不自行產生 send_id。
//      */
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "send_id", nullable = false)
//     private Integer sendId;

//     /**
//      * ------------------------------------------------------------
//      * msgfrom_seller_id
//      * ------------------------------------------------------------
//      *
//      * 訊息發送端 seller_id。
//      *
//      * 依目前規則：
//      *
//      * 系統後台預設：
//      * seller_id = 1
//      *
//      * SC：
//      * 使用目前登入商家的 seller_id
//      *
//      * 這裡暫不建立 @ManyToOne SellerEntity，
//      * 因為 seller 屬於其他模組。
//      *
//      * 後續由 SellerInfo / Member API 驗證。
//      */
//     @Column(name = "msgfrom_seller_id", nullable = false)
//     private Integer msgfromSellerId;

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
//      * 前三碼：
//      * 訊息功能
//      *
//      * 後三碼：
//      * Spring Boot 自動取號
//      *
//      * 自動取號不是 Entity 負責，
//      * 而是 TemplateNumService 負責。
//      */
//     @Column(name = "msg_function", length = 6, nullable = false)
//     private String msgFunction;

//     /**
//      * ------------------------------------------------------------
//      * msg_label
//      * ------------------------------------------------------------
//      *
//      * 樣本儲存者可以自行設定。
//      *
//      * 如果沒有輸入：
//      *
//      * msg_label = send_title
//      *
//      * 由 Spring Boot 控制。
//      */
//     @Column(name = "msg_label", length = 50, nullable = false)
//     private String msgLabel;

//     /**
//      * ------------------------------------------------------------
//      * send_title
//      * ------------------------------------------------------------
//      */
//     @Column(name = "send_title", length = 100, nullable = false)
//     private String sendTitle;

//     /**
//      * ------------------------------------------------------------
//      * send_content
//      * ------------------------------------------------------------
//      */
//     @Column(name = "send_content", length = 1000, nullable = false)
//     private String sendContent;

//     /**
//      * ------------------------------------------------------------
//      * send_upd_at
//      * ------------------------------------------------------------
//      *
//      * INSERT：
//      * SQL Server
//      * DEFAULT SYSDATETIME()
//      *
//      * UPDATE：
//      * Spring Boot
//      * 
//      * @PreUpdate
//      *
//      *            注意：
//      *            這個欄位同時代表新增與更新時間。
//      *
//      *            不建立 record_created_at。
//      */
//     @Column(name = "send_upd_at", nullable = false)
//     @Generated(event = EventType.INSERT)
//     private LocalDateTime sendUpdAt;

//     /**
//      * ------------------------------------------------------------
//      * send_status
//      * ------------------------------------------------------------
//      *
//      * SAVE = 範本
//      * SEND = 實際訊息
//      * DELETE = 軟刪除
//      *
//      * SQL Server 同時有 CHECK：
//      *
//      * SEND
//      * SAVE
//      * DELETE
//      *
//      * Java 端使用 Enum。
//      */
//     @Enumerated(EnumType.STRING)
//     @Column(name = "send_status", length = 10, nullable = false)
//     private SendStatus sendStatus;

//     // ============================================================
//     // Lifecycle Callback
//     // ============================================================

//     /**
//      * INSERT 前處理。
//      *
//      * msg_label 沒有內容時：
//      *
//      * msg_label = send_title
//      *
//      * send_upd_at 不在這裡設定。
//      *
//      * 因為 INSERT 時間指定由 SQL Server：
//      *
//      * DEFAULT SYSDATETIME()
//      */
//     @PrePersist
//     protected void prePersist() {

//         if (isBlank(this.msgLabel)) {
//             this.msgLabel = this.sendTitle;
//         }
//     }

//     /**
//      * UPDATE 前處理。
//      *
//      * send_upd_at 由 Spring Boot 控制。
//      *
//      * 注意：
//      * SQL Server 不負責 UPDATE 時間。
//      */
//     @PreUpdate
//     protected void preUpdate() {

//         this.sendUpdAt = LocalDateTime.now();

//         if (isBlank(this.msgLabel)) {
//             this.msgLabel = this.sendTitle;
//         }
//     }

//     /**
//      * 判斷字串是否為 null 或空白。
//      *
//      * SQL Server 同時使用：
//      *
//      * LEN(LTRIM(RTRIM(...))) > 0
//      *
//      * Java 端先做相同方向的基本檢查，
//      * 但最後資料庫 CHECK 仍然是 DB 保護層。
//      */
//     private boolean isBlank(String value) {

//         return value == null || value.trim().isEmpty();
//     }

//     // ============================================================
//     // Constructor
//     // ============================================================

//     /**
//      * JPA 必須保留無參數 Constructor。
//      *
//      * 若使用 Lombok：
//      *
//      * @NoArgsConstructor
//      *
//      *                    可自動產生。
//      */
//     protected SendEntity() {
//     }

//     /**
//      * 建立 Entity 使用。
//      *
//      * sendId 不放進來，
//      * 因為 send_id 由 SQL Server IDENTITY 產生。
//      */
//     public SendEntity(
//             Integer msgfromSellerId,
//             String msgFunction,
//             String msgLabel,
//             String sendTitle,
//             String sendContent,
//             SendStatus sendStatus) {
//         this.msgfromSellerId = msgfromSellerId;
//         this.msgFunction = msgFunction;
//         this.msgLabel = msgLabel;
//         this.sendTitle = sendTitle;
//         this.sendContent = sendContent;
//         this.sendStatus = sendStatus;
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
//      * 即可自動產生以下方法。
//      */

//     public Integer getSendId() {
//         return sendId;
//     }

//     public void setSendId(Integer sendId) {
//         this.sendId = sendId;
//     }

//     public Integer getMsgfromSellerId() {
//         return msgfromSellerId;
//     }

//     public void setMsgfromSellerId(Integer msgfromSellerId) {
//         this.msgfromSellerId = msgfromSellerId;
//     }

//     public String getMsgFunction() {
//         return msgFunction;
//     }

//     public void setMsgFunction(String msgFunction) {
//         this.msgFunction = msgFunction;
//     }

//     public String getMsgLabel() {
//         return msgLabel;
//     }

//     public void setMsgLabel(String msgLabel) {
//         this.msgLabel = msgLabel;
//     }

//     public String getSendTitle() {
//         return sendTitle;
//     }

//     public void setSendTitle(String sendTitle) {
//         this.sendTitle = sendTitle;
//     }

//     public String getSendContent() {
//         return sendContent;
//     }

//     public void setSendContent(String sendContent) {
//         this.sendContent = sendContent;
//     }

//     public LocalDateTime getSendUpdAt() {
//         return sendUpdAt;
//     }

//     public void setSendUpdAt(LocalDateTime sendUpdAt) {
//         this.sendUpdAt = sendUpdAt;
//     }

//     public SendStatus getSendStatus() {
//         return sendStatus;
//     }

//     public void setSendStatus(SendStatus sendStatus) {
//         this.sendStatus = sendStatus;
//     }
// }