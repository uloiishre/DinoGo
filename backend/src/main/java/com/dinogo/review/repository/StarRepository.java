package com.dinogo.review.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dinogo.review.entity.StarEntity;

@Repository
public interface StarRepository extends JpaRepository<StarEntity, Integer> {

    //review_star外部api// 平均值只計算已填寫 five_star 的評論；無評價時回傳 null。
    @Query("SELECT AVG(s.fiveStar) FROM StarEntity s WHERE s.productId = :productId AND s.fiveStar IS NOT NULL")
    Double findAverageFiveStarByProductId(@Param("productId") Integer productId);

    //review_star外部api// 讓外部模組可同時記錄評分筆數，判斷平均值的有效性。
    long countByProductIdAndFiveStarIsNotNull(Integer productId);

    /** 取得一筆 History 底下的全部商品快照，依 orderItemId 排序。 */
    List<StarEntity> findByHistoryIdOrderByOrderItemIdAsc(Integer historyId);

    /** 修改／清空評論時，同時用 History.memberId 驗證資源所有權。 */
    Optional<StarEntity> findByIdAndHistoryMemberId(Integer starId, Integer memberId);

    /** 第一頁固定取 11 筆；Service 回傳前 10 筆，第 11 筆只判斷 hasNext。 */
    @Query(value = """
            SELECT TOP 11 s.*
            FROM review.star s
            WHERE s.product_id = :productId
              AND s.five_star IS NOT NULL
            ORDER BY
                s.review_priority DESC,
                s.star_upd_at DESC,
                s.star_id DESC
            """, nativeQuery = true)
    List<StarEntity> findFirstProductReviewPage(@Param("productId") Integer productId);

    /*
     * 下一頁依索引排序鍵逐層比較：priority、更新時間、starId。
     * starId 是唯一且穩定的最後 tie-breaker，避免同時間資料重複或遺漏。
     */
    @Query(value = """
            SELECT TOP 11 s.*
            FROM review.star s
            WHERE s.product_id = :productId
              AND s.five_star IS NOT NULL
              AND (
                    s.review_priority < :lastReviewPriority
                    OR (
                        s.review_priority = :lastReviewPriority
                        AND s.star_upd_at < :lastStarUpdAt
                    )
                    OR (
                        s.review_priority = :lastReviewPriority
                        AND s.star_upd_at = :lastStarUpdAt
                        AND s.star_id < :lastStarId
                    )
              )
            ORDER BY
                s.review_priority DESC,
                s.star_upd_at DESC,
                s.star_id DESC
            """, nativeQuery = true)
    List<StarEntity> findNextProductReviewPage(
            @Param("productId") Integer productId,
            @Param("lastReviewPriority") Integer lastReviewPriority,
            @Param("lastStarUpdAt") LocalDateTime lastStarUpdAt,
            @Param("lastStarId") Integer lastStarId);
}
