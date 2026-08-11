package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LoginService loginService;

    @Test
    void loginReturnsMemberWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        Member member = member("$2a$hashed-password", "ACTIVE");
        when(memberRepository.findByEmail(request.email())).thenReturn(java.util.Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPasswordHash())).thenReturn(true);

        LoginResponse response = loginService.login(request);

        assertThat(response.token()).isNull();
        assertThat(response.member().email()).isEqualTo(request.email());
    }

    @Test
    void loginRejectsInvalidPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        Member member = member("$2a$hashed-password", "ACTIVE");
        when(memberRepository.findByEmail(request.email())).thenReturn(java.util.Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");
    }

    @Test
    void loginRejectsMissingOrInactiveMember() {
        LoginRequest missingRequest = new LoginRequest("missing@example.com", "password123");
        when(memberRepository.findByEmail(missingRequest.email())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> loginService.login(missingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");

        LoginRequest inactiveRequest = new LoginRequest("inactive@example.com", "password123");
        Member inactiveMember = member("$2a$hashed-password", "INACTIVE");
        when(memberRepository.findByEmail(inactiveRequest.email())).thenReturn(java.util.Optional.of(inactiveMember));

        assertThatThrownBy(() -> loginService.login(inactiveRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    private Member member(String passwordHash, String status) {
        Member member = new Member();
        member.setMemberId(1);
        member.setEmail("user@example.com");
        member.setPasswordHash(passwordHash);
        member.setLastName("王");
        member.setFirstName("小明");
        member.setStatus(status);
        return member;
    }
}
