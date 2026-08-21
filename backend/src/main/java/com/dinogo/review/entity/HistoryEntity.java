package com.dinogo.review.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 一筆代表會員的一張完成訂單評論紀錄。
 *
 * <p>訂單狀態轉為 COMPLETED 時，由訂單模組資料建立 sellerId、memberId、orderId；
 * 同一個 orderId 只能建立一筆 History。</p>
 */
@Entity
@Table(
        name = "history",
        schema = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_review_history_order",
                columnNames = "order_id"))
@Getter
@NoArgsConstructor
public class HistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer id;

    @Setter
    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Setter
    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @Setter
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    // Spring Boot 只在建立 History 時 cascade persist Star。
    // 刪除 History 時由 SQL Server ON DELETE CASCADE 刪除 Star。
    @OneToMany(
            mappedBy = "history",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.LAZY)
    private List<StarEntity> stars = new ArrayList<>();

    /** Spring Boot 幫助方法：同步建立 History 與 Star 的雙向關聯。 */
    public void addStar(StarEntity star) {
        stars.add(star);
        star.setHistory(this);
    }

    /**
     * 僅解除 Java 物件兩端的關聯，不代表 CANCELLED 訂單的資料庫刪除流程。
     */
    public void removeStar(StarEntity star) {
        stars.remove(star);
        star.setHistory(null);
    }
}
