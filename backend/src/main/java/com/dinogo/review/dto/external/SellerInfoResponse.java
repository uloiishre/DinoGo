package com.dinogo.review.dto.external;

/* 【假設 Seller API Response】其他模組完成後再核對。 */
public record SellerInfoResponse(
        Integer sellerId,
        Integer memberId,
        String storeName,
        String status) {
}
