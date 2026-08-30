package com.dinogo.review.dto.response;

import java.time.LocalDateTime;

import com.dinogo.review.entity.StarEntity;

/**
 * 產品介紹頁使用的公開評論 DTO。
 *
 * <p>//review-前端// 只提供顯示評論所需資料；memberId 由前端遮罩後顯示。</p>
 */
public record ProductReviewResponse(
        Integer starId,
        //review-前端// 提供產品明細頁顯示遮罩後的會員識別碼。
        Integer memberId,
        Integer productId,
        String productName,
        Integer fiveStar,
        String feedback,
        //review-start，總共1次修改，第1次//
        String imgOne,
        String imgTwo,
        String imgThree,
        //review-end，總共1次修改，第1次//
        LocalDateTime starUpdAt) {

    public static ProductReviewResponse fromEntity(StarEntity star) {
        return new ProductReviewResponse(
                star.getId(),
                //review-前端// History 是評論所屬會員的唯一可信來源。
                star.getHistory().getMemberId(),
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
