package com.dinogo.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.dto.RegisterRequest;
import com.dinogo.member.dto.RegisterResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("密碼與確認密碼不一致");
        }

        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email 已被註冊");
        }

        Member member = new Member();
        member.setEmail(request.email());
        member.setPasswordHash(passwordEncoder.encode(request.password()));
        member.setLastName(request.lastName());
        member.setFirstName(request.firstName());
        member.setBirthDate(request.birthDate());
        member.setPhone(request.phone());

        Member savedMember = memberRepository.save(member);
        return new RegisterResponse(MemberResponse.from(savedMember));
    }

    @Transactional(readOnly = true)
    public MemberResponse getProfile(Integer memberId) {
        // 根據 JWT 的 memberId 查詢會員
        return MemberResponse.from(findMemberById(memberId));
    }

    @Transactional
    public MemberResponse updateProfile(Integer memberId, MemberUpdateRequest request) {
        Member member = findMemberById(memberId);
        member.setLastName(request.lastName());
        member.setFirstName(request.firstName());
        member.setBirthDate(request.birthDate());
        member.setPhone(request.phone());

        // 先 flush 觸發 Member 的 @PreUpdate，再將最新修改時間回傳給前端。
        return MemberResponse.from(memberRepository.saveAndFlush(member));
    }

    // 先保留，其他功能若仍需要 email 可以使用
    // private Member findMemberByEmail(String email) {
    // return memberRepository.findByEmail(email)
    // .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    // }

    private Member findMemberById(Integer memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }
}
