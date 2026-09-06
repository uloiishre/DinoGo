package com.dinogo.chat.service;

import java.net.URI;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.dinogo.chat.dto.ChatWebSocketTicketResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

@Service
public class DinoChatWebSocketAuthService {

    private static final long TICKET_TTL_SECONDS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public DinoChatWebSocketAuthService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public ChatWebSocketTicketResponse issueTicket(Integer memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new IllegalArgumentException("Invalid member.");
        }
        cleanupExpiredTickets();
        String ticket = generateTicket();
        Instant expiresAt = Instant.now().plusSeconds(TICKET_TTL_SECONDS);
        tickets.put(ticket, new Ticket(memberId, expiresAt));
        return new ChatWebSocketTicketResponse(ticket, LocalDateTime.now().plusSeconds(TICKET_TTL_SECONDS));
    }

    public Integer authenticate(URI uri) {
        String ticketValue = queryParam(uri, "ticket");
        if (!StringUtils.hasText(ticketValue)) {
            throw new IllegalArgumentException("Invalid ticket.");
        }
        Ticket ticket = tickets.remove(ticketValue);
        if (ticket == null || ticket.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid ticket.");
        }
        Member member = memberRepository.findById(ticket.memberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new IllegalArgumentException("Invalid member.");
        }
        return ticket.memberId();
    }

    private String generateTicket() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanupExpiredTickets() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Ticket>> iterator = tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt().isBefore(now)) {
                iterator.remove();
            }
        }
    }

    private String queryParam(URI uri, String name) {
        String query = uri == null ? null : uri.getRawQuery();
        if (!StringUtils.hasText(query)) return null;
        for (String pair : query.split("&")) {
            int index = pair.indexOf('=');
            if (index <= 0) continue;
            String key = URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8);
            if (name.equals(key)) {
                return URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private record Ticket(Integer memberId, Instant expiresAt) {
    }
}
