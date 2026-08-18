package com.dinogo.member.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String credential) {
}
