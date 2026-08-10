package com.dinogo.sales.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 取消訂單請求，包含取消原因及執行取消的角色。 */
public record CancelOrderRequest(
        @NotBlank(message = "Cancel reason is required")
        @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
        String cancelReason,

        @NotBlank(message = "Cancelled by is required")
        @Pattern(regexp = "BUYER|SELLER|SYSTEM", message = "Cancelled by must be BUYER, SELLER, or SYSTEM")
        String cancelledBy
) {
}
