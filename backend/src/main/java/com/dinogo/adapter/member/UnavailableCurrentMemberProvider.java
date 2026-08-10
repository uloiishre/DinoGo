package com.dinogo.adapter.member;

import org.springframework.stereotype.Component;

import com.dinogo.port.member.CurrentMemberProvider;

@Component
public class UnavailableCurrentMemberProvider implements CurrentMemberProvider {

    @Override
    public Integer requireMemberId() {
        throw new IllegalStateException("Current member authentication is not configured");
    }
}
