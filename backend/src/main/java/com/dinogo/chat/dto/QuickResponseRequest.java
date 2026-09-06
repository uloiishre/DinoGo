package com.dinogo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuickResponseRequest(
        @NotBlank @Size(max = 50) String title,
        @NotBlank @Size(max = 1000) String content) {
}
