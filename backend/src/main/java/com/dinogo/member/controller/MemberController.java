package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.service.MemberService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        try {
            // member.memberId() 是登入者的會員 ID
            return ResponseEntity.ok(
                    memberService.getProfile(member.memberId()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(exception.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody MemberUpdateRequest request) {
        try {
            // 只能修改 JWT 對應的會員
            return ResponseEntity.ok(
                    memberService.updateProfile(member.memberId(), request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(exception.getMessage());
        }
    }
}
