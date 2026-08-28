package com.dinogo.seller.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dinogo.seller.dto.SellerProfileRequest;
import com.dinogo.seller.dto.SellerProfileResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class SellerProfileService {

    private static final long MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024;

    private final SellerRepository sellerRepository;
    private final Cloudinary cloudinary;

    public SellerProfileService(SellerRepository sellerRepository, Cloudinary cloudinary) {
        this.sellerRepository = sellerRepository;
        this.cloudinary = cloudinary;
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
        Seller seller = sellerRepository.findBySellerIdAndStatusIgnoreCase(sellerId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Store not found."));

        return SellerProfileResponse.from(seller);
    }

    // 把前端傳來的 keyword 清理後，去查啟用中的商家，再轉成前端需要的 response。
    @Transactional(readOnly = true)
    public List<SellerProfileResponse> searchPublicStores(String keyword) {
        String safeKeyword = keyword == null ? "" : keyword.trim();

        return sellerRepository
                .findByStoreNameContainingAndStatusIgnoreCase(safeKeyword, "ACTIVE")
                .stream()
                .map(SellerProfileResponse::from)
                .toList();
    }

    @Transactional
    public SellerProfileResponse uploadLogo(Integer memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required.");
        }

        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new IllegalArgumentException("Logo file must be 5 MB or smaller.");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        boolean isImageContentType = contentType != null && contentType.startsWith("image/");
        boolean isImageExtension = originalFilename != null
                && originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp|gif)$");

        if (!isImageContentType || !isImageExtension) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "dinogo/seller-logos",
                            "resource_type", "image"));

            String logoUrl = uploadResult.get("secure_url").toString();
            seller.setStoreLogoUrl(logoUrl);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload seller logo.", exception);
        }

        seller.setUpdatedAt(LocalDateTime.now());

        return SellerProfileResponse.from(sellerRepository.save(seller));
    }
}
