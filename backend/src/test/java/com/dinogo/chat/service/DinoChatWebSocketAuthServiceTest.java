package com.dinogo.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dinogo.chat.dto.ChatWebSocketTicketResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

class DinoChatWebSocketAuthServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final DinoChatWebSocketAuthService service = new DinoChatWebSocketAuthService(memberRepository);

    @Test
    void issuedTicketAuthenticatesMemberOnce() {
        Member member = activeMember(10);
        when(memberRepository.findById(10)).thenReturn(Optional.of(member));

        ChatWebSocketTicketResponse ticket = service.issueTicket(10);

        assertThat(service.authenticate(URI.create("/ws/dino-chat?ticket=" + ticket.ticket()))).isEqualTo(10);
        assertThatThrownBy(() -> service.authenticate(URI.create("/ws/dino-chat?ticket=" + ticket.ticket())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid ticket.");
    }

    @Test
    void loginJwtQueryStringIsNotAcceptedAsWebSocketCredential() {
        assertThatThrownBy(() -> service.authenticate(URI.create("/ws/dino-chat?token=login-jwt")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid ticket.");
    }

    @Test
    void inactiveMemberCannotReceiveTicket() {
        Member member = activeMember(11);
        member.setStatus("SUSPENDED");
        when(memberRepository.findById(11)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.issueTicket(11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid member.");
    }

    private Member activeMember(Integer memberId) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setStatus("ACTIVE");
        return member;
    }
}
