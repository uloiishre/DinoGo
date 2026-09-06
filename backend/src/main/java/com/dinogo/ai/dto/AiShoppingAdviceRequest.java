package com.dinogo.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiShoppingAdviceRequest(
        @NotBlank(message = "請描述想找的商品")
        @Size(max = 500, message = "需求描述不可超過 500 個字") String message) {
}
