package com.dinogo.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.member.repository.RoleRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberRoleRepository memberRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private SellerService sellerService;

    @Test
    void createSellerFromApplicationCreatesSellerSuccessfully() {
        Member member = new Member();
        member.setMemberId(1);

        when(sellerRepository.existsByMember_MemberId(1))
                .thenReturn(false);
        when(memberRepository.findById(1))
                .thenReturn(Optional.of(member));
        Role sellerRole = new Role();
        sellerRole.setRoleId(2);
        sellerRole.setRoleName("seller");
        when(roleRepository.findByRoleName("seller"))
                .thenReturn(Optional.of(sellerRole));
        when(memberRoleRepository.existsByMemberMemberIdAndRoleRoleId(1, 2))
                .thenReturn(false);
        when(sellerRepository.save(any(Seller.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Seller result = sellerService.createSellerFromApplication(
                1,
                "小恐龍商店",
                "專賣恐龍周邊",
                "https://example.com/logo.png");

        assertThat(result).isNotNull();
        assertThat(result.getMember().getMemberId()).isEqualTo(1);
        assertThat(result.getStoreName()).isEqualTo("小恐龍商店");
        assertThat(result.getStoreDescription()).isEqualTo("專賣恐龍周邊");
        assertThat(result.getStoreLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(sellerRepository).existsByMember_MemberId(1);
        verify(memberRepository).findById(1);
        verify(sellerRepository).save(any(Seller.class));
        verify(memberRoleRepository).save(any());
        verify(memberRepository).save(member);
    }

    @Test
    void createSellerFromApplicationRejectsExistingSeller() {
        when(sellerRepository.existsByMember_MemberId(1))
                .thenReturn(true);

        assertThatThrownBy(() -> sellerService.createSellerFromApplication(
                1,
                "小恐龍商店",
                "專賣恐龍周邊",
                "https://example.com/logo.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("該會員已經是商家");

        verify(sellerRepository).existsByMember_MemberId(1);
        verify(memberRepository, never()).findById(any());
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    void createSellerFromApplicationRejectsMissingMember() {
        when(sellerRepository.existsByMember_MemberId(1))
                .thenReturn(false);
        when(memberRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.createSellerFromApplication(
                1,
                "小恐龍商店",
                "專賣恐龍周邊",
                "https://example.com/logo.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("找不到會員");

        verify(sellerRepository).existsByMember_MemberId(1);
        verify(memberRepository).findById(1);
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    void getSellerByMemberIdReturnsSeller() {
        Member member = new Member();
        member.setMemberId(1);

        Seller seller = new Seller();
        seller.setMember(member);
        seller.setStoreName("小恐龍商店");
        seller.setStatus("ACTIVE");

        when(sellerRepository.findByMember_MemberId(1))
                .thenReturn(Optional.of(seller));

        Seller result = sellerService.getSellerByMemberId(1);

        assertThat(result).isSameAs(seller);
        assertThat(result.getMember().getMemberId()).isEqualTo(1);
        assertThat(result.getStoreName()).isEqualTo("小恐龍商店");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(sellerRepository).findByMember_MemberId(1);
    }

    @Test
    void getSellerByMemberIdRejectsMissingSeller() {
        when(sellerRepository.findByMember_MemberId(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.getSellerByMemberId(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("尚未建立商家資料");

        verify(sellerRepository).findByMember_MemberId(1);
    }
}
