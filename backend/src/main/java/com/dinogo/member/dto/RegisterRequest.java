package com.dinogo.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 8, max = 72) String confirmPassword,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 50) String firstName,
        LocalDate birthDate,
        @Size(max = 20) String phone
) {
}
