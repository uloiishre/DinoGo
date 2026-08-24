package com.dinogo.seller.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerWalletResponse;
import com.dinogo.seller.dto.SellerWalletTransactionResponse;
import com.dinogo.seller.service.SellerWalletService;

@RestController
@RequestMapping("/api/seller/wallet")
public class SellerWalletController {

    private final SellerWalletService sellerWalletService;

    public SellerWalletController(SellerWalletService sellerWalletService) {
        this.sellerWalletService = sellerWalletService;
    }

    @GetMapping
    public SellerWalletResponse getWallet(@AuthenticationPrincipal AuthenticatedMember member) {
        return sellerWalletService.getWallet(member.memberId());
    }

    @GetMapping("/transactions")
    public List<SellerWalletTransactionResponse> getTransactions(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return sellerWalletService.getTransactions(member.memberId());
    }
}
