package com.dinogo.seller.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.entity.MemberRoleId;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.member.repository.RoleRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class SellerService {

    private static final String DEFAULT_SELLER_STATUS = "ACTIVE";

    private final SellerRepository sellerRepository;
    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final RoleRepository roleRepository;

    public SellerService(
            SellerRepository sellerRepository,
            MemberRepository memberRepository,
            MemberRoleRepository memberRoleRepository,
            RoleRepository roleRepository) {
        this.sellerRepository = sellerRepository;
        this.memberRepository = memberRepository;
        this.memberRoleRepository = memberRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Seller createSellerFromApplication(
            Integer memberId,
            String storeName,
            String storeDescription,
            String storeLogoUrl) {
        if (sellerRepository.existsByMember_MemberId(memberId)) {
            throw new IllegalArgumentException("該會員已經是商家");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("找不到會員"));
        Role sellerRole = roleRepository.findByRoleName("seller")
                .orElseThrow(() -> new IllegalStateException("Default role 'seller' is not configured"));

        LocalDateTime now = LocalDateTime.now();

        Seller seller = new Seller();
        seller.setMember(member);
        seller.setStoreName(storeName);
        seller.setStoreDescription(storeDescription);
        seller.setStoreLogoUrl(storeLogoUrl);
        seller.setStatus(DEFAULT_SELLER_STATUS);
        seller.setCreatedAt(now);
        seller.setUpdatedAt(now);

        Seller savedSeller = sellerRepository.save(seller);

        if (!memberRoleRepository.existsByMemberMemberIdAndRoleRoleId(memberId, sellerRole.getRoleId())) {
            MemberRole memberRole = new MemberRole();
            memberRole.setId(new MemberRoleId(memberId, sellerRole.getRoleId()));
            memberRole.setMember(member);
            memberRole.setRole(sellerRole);
            memberRoleRepository.save(memberRole);
        }

        member.setAuthVersion(member.getAuthVersion() + 1);
        memberRepository.save(member);
        return savedSeller;
    }

    @Transactional(readOnly = true)
    public Seller getSellerByMemberId(Integer memberId) {
        return sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("尚未建立商家資料"));
    }
}
