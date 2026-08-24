package com.dinogo.review.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.review.entity.StarEntity;

public interface StarRepository extends JpaRepository<StarEntity, Integer> {

        // 單體內商品模組可呼叫 ReviewService 取得摘要；平均值只計算已填寫的 five_star。
        @Query("SELECT AVG(s.fiveStar) FROM StarEntity s WHERE s.productId = :productId AND s.fiveStar IS NOT NULL")
        Double findAverageFiveStarByProductId(@Param("productId") Integer productId);

        // 評分筆數供同一應用內的商品模組判斷平均值是否有效。
        long countByProductIdAndFiveStarIsNotNull(Integer productId);

        /*
         * 廠商評價只統計目前仍上架且確實售出的商品。Star 與 History 分別證明
         * 評分及訂單完成，catalog.Product 則提供即時的上架與銷售狀態。
         */
        @Query(value = """
                        SELECT AVG(CAST(s.five_star AS decimal(10, 4)))
                        FROM review.star s
                        INNER JOIN review.history h ON h.history_id = s.history_id
                        INNER JOIN catalog.Product p ON p.product_id = s.product_id
                        WHERE h.seller_id = :sellerId
                          AND p.seller_id = :sellerId
                          AND p.status = 1
                          AND p.sold_count > 0
                          AND s.five_star IS NOT NULL
                        """, nativeQuery = true)
        Double findPublishedSoldProductAverageFiveStarBySellerId(
                        @Param("sellerId") Integer sellerId);

        @Query(value = """
                        SELECT COUNT_BIG(*)
                        FROM review.star s
                        INNER JOIN review.history h ON h.history_id = s.history_id
                        INNER JOIN catalog.Product p ON p.product_id = s.product_id
                        WHERE h.seller_id = :sellerId
                          AND p.seller_id = :sellerId
                          AND p.status = 1
                          AND p.sold_count > 0
                          AND s.five_star IS NOT NULL
                        """, nativeQuery = true)
        long countPublishedSoldProductRatingsBySellerId(@Param("sellerId") Integer sellerId);

        @Query(value = """
                        SELECT COUNT_BIG(DISTINCT s.product_id)
                        FROM review.star s
                        INNER JOIN review.history h ON h.history_id = s.history_id
                        INNER JOIN catalog.Product p ON p.product_id = s.product_id
                        WHERE h.seller_id = :sellerId
                          AND p.seller_id = :sellerId
                          AND p.status = 1
                          AND p.sold_count > 0
                          AND s.five_star IS NOT NULL
                        """, nativeQuery = true)
        long countPublishedSoldRatedProductsBySellerId(@Param("sellerId") Integer sellerId);

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
