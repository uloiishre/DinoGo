package com.dinogo.review.dto.response;

import java.math.BigDecimal;

//review_star外部api// 提供外部模組同步商品評價統計，不暴露 Review Entity。
public record ProductRatingSummaryResponse(
        Integer productId,
        BigDecimal averageFiveStar,
        long ratingCount) {
}
