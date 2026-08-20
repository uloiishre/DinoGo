package com.dinogo.member.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.seller.repository.SellerRepository;

/**
 * In-process member provider for system-message and review integrations.
 */
@Service
public class MemberSysmsgProviderService {

    private static final String DEFAULT_ROLE = "MEMBER";

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;

    public MemberSysmsgProviderService(
            MemberRepository memberRepository,
            SellerRepository sellerRepository) {
        this.memberRepository = memberRepository;
        this.sellerRepository = sellerRepository;
    }

    /**
     * Supplies data for the authenticated member resolved by the caller's JWT.
     */
    @Transactional(readOnly = true)
    public MemberSysmsgResponse getProfile(Integer memberId) {
        return toResponse(requireActiveMember(memberId), true);
    }

    /**
     * Supplies an active recipient for system messages and review validation.
     */
    @Transactional(readOnly = true)
    public MemberSysmsgResponse getMember(Integer memberId) {
        return toResponse(requireActiveMember(memberId), false);
    }

    /**
     * Supplies all active recipients for official-account broadcasts.
     */
    @Transactional(readOnly = true)
    public List<MemberSysmsgResponse> getAllMembers() {
        List<Member> members = memberRepository.findAllByStatusIgnoreCase("ACTIVE");
        Map<Integer, Integer> sellerIdsByMemberId = members.isEmpty()
                ? Map.of()
                : sellerRepository.findByMember_MemberIdIn(members.stream()
                                .map(Member::getMemberId)
                                .toList())
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                seller -> seller.getMember().getMemberId(),
                                seller -> seller.getSellerId()));

        return members.stream()
                .map(member -> toResponse(member, sellerIdsByMemberId.get(member.getMemberId()), false))
                .toList();
    }

    private Member requireActiveMember(Integer memberId) {
        return memberRepository.findById(memberId)
                .filter(this::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
    }

    private boolean isActive(Member member) {
        return "ACTIVE".equalsIgnoreCase(member.getStatus());
    }

    private MemberSysmsgResponse toResponse(Member member, boolean authenticated) {
        Integer sellerId = sellerRepository.findByMember_MemberId(member.getMemberId())
                .map(seller -> seller.getSellerId())
                .orElse(null);
        return toResponse(member, sellerId, authenticated);
    }

    private MemberSysmsgResponse toResponse(
            Member member,
            Integer sellerId,
            boolean authenticated) {
        String role = member.getMemberRoles().stream()
                .map(memberRole -> memberRole.getRole().getRoleName())
                .findFirst()
                .orElse(DEFAULT_ROLE);

        return new MemberSysmsgResponse(
                member.getMemberId(),
                sellerId,
                authenticated,
                member.getEmail(),
                role,
                member.isEmailOrderNotifications(),
                member.isEmailMarketingNotifications());
    }
}
