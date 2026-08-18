package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.GoogleLinkRequest;
import com.dinogo.member.dto.GoogleLoginRequest;
import com.dinogo.member.service.GoogleAccountLinkRequiredException;
import com.dinogo.member.service.GoogleLoginService;
import com.dinogo.member.service.LoginService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;
    private final GoogleLoginService googleLoginService;

    public LoginController(LoginService loginService, GoogleLoginService googleLoginService) {
        this.loginService = loginService;
        this.googleLoginService = googleLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(loginService.login(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MemberApiErrorResponse.from(
                            HttpStatus.UNAUTHORIZED,
                            "Email 或密碼錯誤"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            return ResponseEntity.ok(googleLoginService.login(request));
        } catch (GoogleAccountLinkRequiredException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(MemberApiErrorResponse.from(HttpStatus.CONFLICT, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MemberApiErrorResponse.from(HttpStatus.UNAUTHORIZED, exception.getMessage()));
        }
    }

    @PostMapping("/google/link")
    public ResponseEntity<?> linkGoogleAccount(@Valid @RequestBody GoogleLinkRequest request) {
        try {
            return ResponseEntity.ok(googleLoginService.link(request));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(MemberApiErrorResponse.from(HttpStatus.CONFLICT, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MemberApiErrorResponse.from(HttpStatus.UNAUTHORIZED, exception.getMessage()));
        }
    }
}
