package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.PasswordResetRequest;
import com.dinogo.member.dto.ResetPasswordRequest;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.PasswordResetToken;
import com.dinogo.security.PasswordResetTokenService;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private PasswordResetMailService passwordResetMailService;

    @Mock
    private PasswordResetRateLimiter passwordResetRateLimiter;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestSendsResetEmailForActiveMember() {
        Member member = member(1, 4);
        when(memberRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(member));
        when(passwordResetTokenService.create(member)).thenReturn("reset-token");

        passwordResetService.requestPasswordReset(new PasswordResetRequest("User@Example.COM"), "127.0.0.1");

        verify(passwordResetRateLimiter).check("user@example.com", "127.0.0.1");
        verify(passwordResetMailService).sendPasswordReset("user@example.com", "reset-token");
    }

    @Test
    void requestDoesNotSendEmailForUnknownOrInactiveMember() {
        when(memberRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset(new PasswordResetRequest("missing@example.com"), "127.0.0.1");

        verify(passwordResetMailService, never()).sendPasswordReset(any(), any());
        verify(passwordResetTokenService, never()).create(any());
    }

    @Test
    void resetChangesPasswordAndInvalidatesExistingTokens() {
        Member member = member(1, 4);
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "new-password", "new-password");
        when(passwordResetTokenService.parse("reset-token"))
                .thenReturn(new PasswordResetToken(1, "user@example.com", 4));
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        passwordResetService.resetPassword(request);

        assertThat(member.getPasswordHash()).isEqualTo("new-hash");
        assertThat(member.getAuthVersion()).isEqualTo(5);
        verify(memberRepository).saveAndFlush(member);
    }

    @Test
    void resetRejectsTokenIssuedBeforeAnotherPasswordChange() {
        Member member = member(1, 5);
        when(passwordResetTokenService.parse("reset-token"))
                .thenReturn(new PasswordResetToken(1, "user@example.com", 4));
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("reset-token", "new-password", "new-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("重設連結無效或已過期");
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    private Member member(int memberId, int authVersion) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setEmail("user@example.com");
        member.setPasswordHash("old-hash");
        member.setStatus("ACTIVE");
        member.setAuthVersion(authVersion);
        return member;
    }
}
