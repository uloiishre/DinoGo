package com.dinogo.port.member;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.dinogo.member.entity.Member;

@Component
public class CurrentMemberProviderAdapter implements CurrentMemberProvider {

    @Override
    public Integer requireMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated member available");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Member member && member.getMemberId() != null) {
            return member.getMemberId();
        }
        if (principal instanceof Integer integer) {
            return integer;
        }
        if (principal instanceof String text) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                // intentionally ignored; handled below
            }
        }

        throw new IllegalStateException("Unable to resolve current member ID from security context");
    }
}
