package com.dinogo.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改單筆評論的 Request DTO。
 *
 * <p>memberId 不放在 Request Body；會員身分由 Authorization 對應的登入模組取得，
 * 不信任前端自行傳入的 memberId。圖片 byte[] 在 JSON 中以 Base64 字串傳送。</p>
 */
public record StarUpdateRequest(
        @NotNull(message = "fiveStar 不可為空")
        @Min(value = 1, message = "fiveStar 最小為 1")
        @Max(value = 5, message = "fiveStar 最大為 5")
        Integer fiveStar,

        @Size(max = 500, message = "feedback 最多 500 字")
        String feedback,

        byte[] imgOne,
        byte[] imgTwo,
        byte[] imgThree) {
}
