package com.dinogo.review.dto.response;

import java.time.LocalDateTime;

import com.dinogo.review.entity.StarEntity;

/**
 * 產品介紹頁使用的公開評論 DTO。
 *
 * <p>公開回應不包含原始 memberId；匿名名稱由後端直接提供。</p>
 */
public record ProductReviewResponse(
        Integer starId,
        //review-start，總共3次修改，第1次//
        String reviewerDisplayName,
        //review-end，總共3次修改，第1次//
        Integer productId,
        String productName,
        Integer fiveStar,
        String feedback,
        //review-start，總共3次修改，第2次//
        String imgOne,
        String imgTwo,
        String imgThree,
        //review-end，總共3次修改，第2次//
        LocalDateTime starUpdAt) {

    public static ProductReviewResponse fromEntity(StarEntity star) {
        return new ProductReviewResponse(
                star.getId(),
                //review-start，總共3次修改，第3次//
                // 不由低熵 memberId 產生可枚舉雜湊，也不提供跨評論追蹤識別碼。
                "匿名會員",
                //review-end，總共3次修改，第3次//
                star.getProductId(),
                star.getProductName(),
                star.getFiveStar(),
                star.getFeedback(),
                star.getImgOne(),
                star.getImgTwo(),
                star.getImgThree(),
                star.getStarUpdAt());
    }
}

