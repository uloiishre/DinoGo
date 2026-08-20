package com.dinogo.seller.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerProfileRequest;
import com.dinogo.seller.dto.SellerProfileResponse;
import com.dinogo.seller.service.SellerProfileService;

@RestController
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    public SellerProfileController(SellerProfileService sellerProfileService) {
        this.sellerProfileService = sellerProfileService;
    }

    @GetMapping("/api/seller/profile")
    public SellerProfileResponse getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return sellerProfileService.getMyProfile(member.memberId());
    }

    @PutMapping("/api/seller/profile")
    public SellerProfileResponse updateMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestBody SellerProfileRequest request) {
        return sellerProfileService.updateMyProfile(member.memberId(), request);
    }

    @GetMapping("/api/stores/{sellerId}")
    public SellerProfileResponse getPublicStore(
            @org.springframework.web.bind.annotation.PathVariable Integer sellerId) {
        return sellerProfileService.getPublicStore(sellerId);
    }
}