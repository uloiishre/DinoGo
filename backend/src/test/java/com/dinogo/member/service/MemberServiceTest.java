package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.dto.ChangePasswordRequest;
import com.dinogo.member.dto.RegisterRequest;
import com.dinogo.member.dto.RegisterResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.member.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MemberRoleRepository memberRoleRepository;

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
                "0912345678");
    }

    @Test
    void registerHashesPasswordBeforeSaving() {
        Role buyerRole = buyerRole();
        when(memberRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(roleRepository.findByRoleName("buyer")).thenReturn(Optional.of(buyerRole));
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

        ArgumentCaptor<MemberRole> memberRoleCaptor = ArgumentCaptor.forClass(MemberRole.class);
        verify(memberRoleRepository).save(memberRoleCaptor.capture());
        MemberRole savedMemberRole = memberRoleCaptor.getValue();
        assertThat(savedMemberRole.getId().getMemberId()).isEqualTo(1);
        assertThat(savedMemberRole.getId().getRoleId()).isEqualTo(1);
        assertThat(savedMemberRole.getMember()).isSameAs(captor.getValue());
        assertThat(savedMemberRole.getRole()).isSameAs(buyerRole);
    }

    @Test
    void registerFailsWhenDefaultBuyerRoleIsMissing() {
        when(memberRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(roleRepository.findByRoleName("buyer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Default role 'buyer' is not configured");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(memberRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 已被註冊");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
        verify(roleRepository, never()).findByRoleName(any());
        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void registerNormalizesEmailBeforeCheckingAndSaving() {
        RegisterRequest mixedCaseRequest = new RegisterRequest(
                "User@Example.COM", "password123", "password123", "王", "小明", null, null);
        when(memberRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(roleRepository.findByRoleName("buyer")).thenReturn(Optional.of(buyerRole()));
        when(passwordEncoder.encode(mixedCaseRequest.password())).thenReturn("hashed-password");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setMemberId(1);
            return member;
        });

        memberService.register(mixedCaseRequest);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        RegisterRequest mismatchedRequest = new RegisterRequest(
                request.email(), request.password(), "different-password",
                request.lastName(), request.firstName(), request.birthDate(), request.phone());

        assertThatThrownBy(() -> memberService.register(mismatchedRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("密碼與確認密碼不一致");

        verify(memberRepository, never()).existsByEmailIgnoreCase(any());
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
        verify(roleRepository, never()).findByRoleName(any());
        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void getProfileFindsMemberByAuthenticatedMemberId() {
        Member member = new Member();
        member.setMemberId(1);
        member.setEmail("user@example.com");
        member.setLastName("王");
        member.setFirstName("小明");
        member.setCreatedAt(LocalDateTime.of(2025, 8, 18, 10, 0));
        member.setUpdatedAt(LocalDateTime.of(2025, 8, 20, 9, 30));
        when(memberRepository.findById(member.getMemberId()))
                .thenReturn(Optional.of(member));

        MemberResponse response = memberService.getProfile(member.getMemberId());

        assertThat(response.memberId()).isEqualTo(1);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2025, 8, 18, 10, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2025, 8, 20, 9, 30));
        // 會員身份由 JWT 的 memberId 決定，Service 應使用 findById 查詢。
        verify(memberRepository).findById(member.getMemberId());
    }

    @Test
    void updateProfileChangesOnlyEditableFields() {
        Member member = new Member();
        member.setMemberId(1);
        member.setEmail("user@example.com");
        member.setPasswordHash("hashed-password");
        member.setLastName("王");
        member.setFirstName("小明");
        member.setCreatedAt(LocalDateTime.of(2025, 8, 18, 10, 0));
        member.setUpdatedAt(LocalDateTime.of(2025, 8, 20, 9, 30));
        // 更新資料時同樣使用 JWT 的 memberId，不使用 email 查詢。
        when(memberRepository.findById(member.getMemberId()))
                .thenReturn(Optional.of(member));
        when(memberRepository.saveAndFlush(any(Member.class))).thenAnswer(invocation -> {
            Member savedMember = invocation.getArgument(0);
            savedMember.setUpdatedAt(LocalDateTime.of(2026, 8, 14, 15, 10));
            return savedMember;
        });

        MemberUpdateRequest request = new MemberUpdateRequest(
                "林", "小美", LocalDate.of(1998, 2, 3), "0987654321");

        MemberResponse response = memberService.updateProfile(member.getMemberId(), request);

        assertThat(response.lastName()).isEqualTo("林");
        assertThat(response.firstName()).isEqualTo("小美");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1998, 2, 3));
        assertThat(response.phone()).isEqualTo("0987654321");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2025, 8, 18, 10, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 14, 15, 10));
        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getPasswordHash()).isEqualTo("hashed-password");
        verify(memberRepository).saveAndFlush(member);
    }

    @Test
    void changePasswordHashesNewPasswordAndInvalidatesExistingTokens() {
        Member member = new Member();
        member.setMemberId(1);
        member.setPasswordHash("old-hash");
        member.setAuthVersion(4);
        ChangePasswordRequest request = new ChangePasswordRequest(
                "current-password", "new-password", "new-password");
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        memberService.changePassword(1, request);

        assertThat(member.getPasswordHash()).isEqualTo("new-hash");
        assertThat(member.getAuthVersion()).isEqualTo(5);
        verify(memberRepository).saveAndFlush(member);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        Member member = new Member();
        member.setMemberId(1);
        member.setPasswordHash("old-hash");
        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrong-password", "new-password", "new-password");
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> memberService.changePassword(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("目前密碼錯誤");
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    private Role buyerRole() {
        Role role = new Role();
        role.setRoleId(1);
        role.setRoleName("buyer");
        return role;
    }
}
