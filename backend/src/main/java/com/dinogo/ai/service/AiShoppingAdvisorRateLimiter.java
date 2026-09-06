package com.dinogo.ai.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiShoppingAdvisorRateLimiter {
    private final int maxRequests;
    private final Duration window;
    private final Map<Integer, Deque<Instant>> requestsByMember = new ConcurrentHashMap<>();

    public AiShoppingAdvisorRateLimiter(
            @Value("${app.ai.shopping-advisor.rate-limit-max-requests:10}") int maxRequests,
            @Value("${app.ai.shopping-advisor.rate-limit-window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public void check(Integer memberId) {
        Instant now = Instant.now();
        Deque<Instant> timestamps = requestsByMember.computeIfAbsent(memberId, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().plus(window).isBefore(now)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxRequests) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI 顧問請求過於頻繁，請稍後再試。");
            }
            timestamps.addLast(now);
        }
    }
}
