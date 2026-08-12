package com.dinogo.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.service.MemberService;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken("user@example.com", null);

    @Test
    void getProfileReturnsAuthenticatedMember() {
        MemberResponse response = new MemberResponse(
                1, "user@example.com", "王", "小明", null, "0912345678", "ACTIVE");
        when(memberService.getProfile("user@example.com")).thenReturn(response);

        ResponseEntity<?> result = memberController.getProfile(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(memberService).getProfile("user@example.com");
    }

    @Test
    void updateProfileReturnsUpdatedMember() {
        MemberUpdateRequest request = new MemberUpdateRequest(
                "林", "小美", LocalDate.of(1998, 2, 3), "0987654321");
        MemberResponse response = new MemberResponse(
                1, "user@example.com", "林", "小美", request.birthDate(), request.phone(), "ACTIVE");
        when(memberService.updateProfile("user@example.com", request)).thenReturn(response);

        ResponseEntity<?> result = memberController.updateProfile(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(memberService).updateProfile("user@example.com", request);
    }

    @Test
    void getProfileReturnsNotFoundWhenMemberDoesNotExist() {
        when(memberService.getProfile("user@example.com"))
                .thenThrow(new IllegalArgumentException("Member not found"));

        ResponseEntity<?> result = memberController.getProfile(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo("Member not found");
    }

    @Test
    void updateProfileReturnsNotFoundWhenMemberDoesNotExist() {
        MemberUpdateRequest request = new MemberUpdateRequest(
                "林", "小美", null, null);
        when(memberService.updateProfile("user@example.com", request))
                .thenThrow(new IllegalArgumentException("Member not found"));

        ResponseEntity<?> result = memberController.updateProfile(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo("Member not found");
    }
}
