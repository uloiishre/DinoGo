package com.dinogo.member.dto;

public record LoginResponse(
        String token,
        MemberResponse member
) {
}
