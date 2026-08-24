package com.dinogo.member.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.PasswordResetRequest;
import com.dinogo.member.dto.ResetPasswordRequest;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.PasswordResetToken;
import com.dinogo.security.PasswordResetTokenService;

@Service
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordResetRateLimiter passwordResetRateLimiter;

    public PasswordResetService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenService passwordResetTokenService,
            PasswordResetMailService passwordResetMailService,
            PasswordResetRateLimiter passwordResetRateLimiter) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordResetMailService = passwordResetMailService;
        this.passwordResetRateLimiter = passwordResetRateLimiter;
    }

    @Transactional(readOnly = true)
    public void requestPasswordReset(PasswordResetRequest request, String clientIp) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        passwordResetRateLimiter.check(email, clientIp);
        memberRepository.findByEmailIgnoreCase(email)
                .filter(member -> "ACTIVE".equals(member.getStatus()))
                .ifPresent(member -> passwordResetMailService.sendPasswordReset(
                        member.getEmail(), passwordResetTokenService.create(member)));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("新密碼與確認密碼不一致");
        }

        PasswordResetToken token = passwordResetTokenService.parse(request.token());
        Member member = memberRepository.findById(token.memberId())
                .orElseThrow(this::invalidToken);
        if (!"ACTIVE".equals(member.getStatus())
                || !member.getEmail().equalsIgnoreCase(token.email())
                || member.getAuthVersion() != token.authVersion()) {
            throw invalidToken();
        }

        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        member.setAuthVersion(Math.incrementExact(member.getAuthVersion()));
        memberRepository.saveAndFlush(member);
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("重設連結無效或已過期");
    }
}
