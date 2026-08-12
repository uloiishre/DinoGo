package com.dinogo.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 50) String firstName,
        LocalDate birthDate,
        @Size(max = 20) String phone
) {
}
