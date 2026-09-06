package com.dinogo.ai.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AiShoppingAdvisorRateLimiterTest {
    @Test
    void rejectsTheThirdRequestInTheSameWindow() {
        AiShoppingAdvisorRateLimiter limiter = new AiShoppingAdvisorRateLimiter(2, 60);
        assertDoesNotThrow(() -> limiter.check(1));
        assertDoesNotThrow(() -> limiter.check(1));
        assertThrows(ResponseStatusException.class, () -> limiter.check(1));
    }

    @Test
    void keepsEachMemberInAnIndependentWindow() {
        AiShoppingAdvisorRateLimiter limiter = new AiShoppingAdvisorRateLimiter(1, 60);
        limiter.check(1);
        assertDoesNotThrow(() -> limiter.check(2));
    }
}
