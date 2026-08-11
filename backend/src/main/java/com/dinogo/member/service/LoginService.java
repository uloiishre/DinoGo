package com.dinogo.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.JwtTokenUtil;

@Service
public class LoginService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public LoginService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email 或密碼錯誤"));

        if (!"ACTIVE".equals(member.getStatus())
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new IllegalArgumentException("Email 或密碼錯誤");
        }

        String token = jwtTokenUtil.generateToken(member.getEmail(), member.getMemberId());
        return new LoginResponse(token, MemberResponse.from(member));
    }
}
