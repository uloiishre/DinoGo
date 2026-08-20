package com.dinogo.seller.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.seller.dto.SellerProfileRequest;
import com.dinogo.seller.dto.SellerProfileResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class SellerProfileService {

    private final SellerRepository sellerRepository;

    public SellerProfileService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getMyProfile(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        return SellerProfileResponse.from(seller);
    }

    @Transactional
    public SellerProfileResponse updateMyProfile(Integer memberId, SellerProfileRequest request) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        seller.setStoreName(request.storeName());
        seller.setStoreDescription(request.storeDescription());
        seller.setStoreLogoUrl(request.storeLogoUrl());
        seller.setStatus(request.status());
        seller.setServiceStartTime(request.serviceStartTime());
        seller.setServiceEndTime(request.serviceEndTime());
        seller.setUpdatedAt(LocalDateTime.now());
        return SellerProfileResponse.from(sellerRepository.save(seller));
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getPublicStore(Integer sellerId) {
        Seller seller = sellerRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found."));

        return SellerProfileResponse.from(seller);
    }
}