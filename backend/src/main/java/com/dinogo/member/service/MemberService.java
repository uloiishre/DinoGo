package com.dinogo.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.MemberResponse;
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
}
