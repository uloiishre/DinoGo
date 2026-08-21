package com.dinogo.review.dto.response;

import java.util.List;

/**
 * 產品評論 Keyset Pagination 回傳資料。
 *
 * @param content 實際回傳的評論，最多 10 筆
 * @param hasNext 是否還有下一頁
 * @param nextCursor 下一頁游標；hasNext 為 false 時是 null
 */
public record ProductReviewPageResponse(
        List<ProductReviewResponse> content,
        boolean hasNext,
        ProductReviewCursor nextCursor) {
}
