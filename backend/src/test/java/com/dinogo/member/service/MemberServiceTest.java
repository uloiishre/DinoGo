package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.RegisterRequest;
import com.dinogo.member.dto.RegisterResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest(
                "user@example.com",
                "password123",
                "password123",
                "王",
                "小明",
                LocalDate.of(2000, 1, 1),
                "0912345678"
        );
    }

    @Test
    void registerHashesPasswordBeforeSaving() {
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("$2a$hashed-password");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setMemberId(1);
            return member;
        });

        RegisterResponse response = memberService.register(request);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$hashed-password");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo(request.password());
        assertThat(response.member().memberId()).isEqualTo(1);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 已被註冊");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        RegisterRequest mismatchedRequest = new RegisterRequest(
                request.email(), request.password(), "different-password",
                request.lastName(), request.firstName(), request.birthDate(), request.phone()
        );

        assertThatThrownBy(() -> memberService.register(mismatchedRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("密碼與確認密碼不一致");

        verify(memberRepository, never()).existsByEmail(any());
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
    }
}
