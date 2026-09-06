package com.dinogo.chat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.chat.dto.QuickResponseRequest;
import com.dinogo.chat.dto.QuickResponseResponse;
import com.dinogo.chat.service.QuickResponseService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/chat/quick-responses")
public class QuickResponseController {

    private final QuickResponseService service;

    public QuickResponseController(QuickResponseService service) {
        this.service = service;
    }

    @GetMapping
    public List<QuickResponseResponse> list(@AuthenticationPrincipal AuthenticatedMember member) {
        return service.list(member.memberId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuickResponseResponse create(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody QuickResponseRequest request) {
        return service.create(member.memberId(), request);
    }

    @PutMapping("/{templateId}")
    public QuickResponseResponse update(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer templateId,
            @Valid @RequestBody QuickResponseRequest request) {
        return service.update(member.memberId(), templateId, request);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer templateId) {
        service.delete(member.memberId(), templateId);
    }
}
