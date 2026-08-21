package com.dinogo.review.dto.response;

import java.time.LocalDateTime;

import com.dinogo.review.entity.StarEntity;

/**
 * 產品介紹頁使用的公開評論 DTO。
 *
 * <p>只提供顯示評論所需資料，不回傳 memberId、sellerId 或其他會員敏感資訊。</p>
 */
public record ProductReviewResponse(
        Integer starId,
        Integer productId,
        String productName,
        Integer fiveStar,
        String feedback,
        byte[] imgOne,
        byte[] imgTwo,
        byte[] imgThree,
        LocalDateTime starUpdAt) {

    public static ProductReviewResponse fromEntity(StarEntity star) {
        return new ProductReviewResponse(
                star.getId(),
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
