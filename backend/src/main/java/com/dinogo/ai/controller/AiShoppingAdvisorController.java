package com.dinogo.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.ai.dto.AiShoppingAdviceRequest;
import com.dinogo.ai.dto.AiShoppingAdviceResponse;
import com.dinogo.ai.service.AiShoppingAdvisorService;
import com.dinogo.ai.service.AiShoppingAdvisorRateLimiter;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/ai-shopping-advisor")
public class AiShoppingAdvisorController {
    private final AiShoppingAdvisorService aiShoppingAdvisorService;
    private final AiShoppingAdvisorRateLimiter rateLimiter;
    public AiShoppingAdvisorController(AiShoppingAdvisorService aiShoppingAdvisorService, AiShoppingAdvisorRateLimiter rateLimiter) {
        this.aiShoppingAdvisorService = aiShoppingAdvisorService;
        this.rateLimiter = rateLimiter;
    }
    @PostMapping
    public ResponseEntity<AiShoppingAdviceResponse> advise(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody AiShoppingAdviceRequest request) {
        rateLimiter.check(member.memberId());
        return ResponseEntity.ok(aiShoppingAdvisorService.advise(request.message()));
    }
}
