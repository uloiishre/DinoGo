package com.dinogo.member.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLinkRequest(
        @NotBlank String credential,
        @NotBlank String password) {
}
