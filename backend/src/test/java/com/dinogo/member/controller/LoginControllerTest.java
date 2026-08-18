package com.dinogo.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.GoogleLoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.service.LoginService;
import com.dinogo.member.service.GoogleLoginService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;

    @Mock
    private GoogleLoginService googleLoginService;

    @InjectMocks
    private LoginController loginController;

    @Test
    void loginReturnsTokenAndMemberWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        LoginResponse response = new LoginResponse(
                "jwt-token",
                new MemberResponse(1, "user@example.com", "王", "小明", null, null, "ACTIVE"),
                List.of("buyer"));
        when(loginService.login(request)).thenReturn(response);

        ResponseEntity<?> result = loginController.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(loginService).login(request);
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        when(loginService.login(request)).thenThrow(new IllegalArgumentException("Email 或密碼錯誤"));

        ResponseEntity<?> result = loginController.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                HttpStatus.UNAUTHORIZED,
                "Email 或密碼錯誤"));
    }

    @Test
    void googleLoginReturnsConflictWhenAccountLinkIsRequired() {
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-token");
        when(googleLoginService.login(request))
                .thenThrow(new com.dinogo.member.service.GoogleAccountLinkRequiredException());

        ResponseEntity<?> result = loginController.googleLogin(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                HttpStatus.CONFLICT,
                "此 Email 已有密碼帳號，請輸入原密碼完成 Google 帳號綁定"));
    }
}
