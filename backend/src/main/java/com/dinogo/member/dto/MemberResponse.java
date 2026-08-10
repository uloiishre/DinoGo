package com.dinogo.member.dto;

import java.time.LocalDate;

import com.dinogo.member.entity.Member;

public record MemberResponse(
        Integer memberId,
        String email,
        String lastName,
        String firstName,
        LocalDate birthDate,
        String phone,
        String status
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getLastName(),
                member.getFirstName(),
                member.getBirthDate(),
                member.getPhone(),
                member.getStatus()
        );
    }
}
