package com.dinogo.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
public class LoginService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email 或密碼錯誤"));

        if (!"ACTIVE".equals(member.getStatus())
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new IllegalArgumentException("Email 或密碼錯誤");
        }

        // JWT 尚未加入前，先回傳已驗證的會員資料；token 暫時為 null。
        return new LoginResponse(null, MemberResponse.from(member));
    }
}
