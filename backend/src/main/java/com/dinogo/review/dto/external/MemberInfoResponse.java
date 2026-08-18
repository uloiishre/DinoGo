package com.dinogo.review.dto.external;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 對應 member 模組 MemberResponse。
public record MemberInfoResponse(
        Integer memberId,
        String email,
        String lastName,
        String firstName,
        LocalDate birthDate,
        String phone,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
