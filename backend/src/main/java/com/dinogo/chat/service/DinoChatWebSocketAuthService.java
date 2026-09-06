package com.dinogo.chat.service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.JwtTokenUtil;

@Service
public class DinoChatWebSocketAuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final MemberRepository memberRepository;

    public DinoChatWebSocketAuthService(JwtTokenUtil jwtTokenUtil, MemberRepository memberRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.memberRepository = memberRepository;
    }

    public Integer authenticate(URI uri) {
        String token = queryParam(uri, "token");
        if (!StringUtils.hasText(token) || !jwtTokenUtil.isValid(token)) {
            throw new IllegalArgumentException("Invalid token.");
        }
        Integer memberId = jwtTokenUtil.extractMemberId(token);
        int authVersion = jwtTokenUtil.extractAuthVersion(token);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (!"ACTIVE".equals(member.getStatus()) || member.getAuthVersion() != authVersion) {
            throw new IllegalArgumentException("Invalid member.");
        }
        return memberId;
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
}
