package com.dinogo.chat.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.dinogo.chat.dto.QuickResponseRequest;
import com.dinogo.chat.dto.QuickResponseResponse;
import com.dinogo.chat.entity.ChatQuickResponseTemplate;
import com.dinogo.seller.service.CurrentSellerService;

@Service
public class QuickResponseService {

    private final CurrentSellerService currentSellerService;
    private final AtomicInteger templateSequence = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, ChatQuickResponseTemplate> templates = new ConcurrentHashMap<>();

    public QuickResponseService(CurrentSellerService currentSellerService) {
        this.currentSellerService = currentSellerService;
    }

    public List<QuickResponseResponse> list(Integer memberId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        return templates.values().stream()
                .filter(template -> sellerId.equals(template.getSellerId()))
                .sorted(Comparator
                        .comparing(ChatQuickResponseTemplate::getUpdatedAt)
                        .reversed()
                        .thenComparing(ChatQuickResponseTemplate::getTemplateId, Comparator.reverseOrder()))
                .map(QuickResponseResponse::from)
                .toList();
    }

    public QuickResponseResponse create(Integer memberId, QuickResponseRequest request) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        LocalDateTime now = LocalDateTime.now();
        ChatQuickResponseTemplate template = new ChatQuickResponseTemplate();
        template.setTemplateId(templateSequence.getAndIncrement());
        template.setSellerId(sellerId);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        apply(template, request);
        templates.put(template.getTemplateId(), template);
        return QuickResponseResponse.from(template);
    }

    public QuickResponseResponse update(Integer memberId, Integer templateId, QuickResponseRequest request) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        ChatQuickResponseTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Quick response not found.");
        }
        if (!sellerId.equals(template.getSellerId())) {
            throw new IllegalArgumentException("No permission to update this quick response.");
        }
        apply(template, request);
        template.setUpdatedAt(LocalDateTime.now());
        return QuickResponseResponse.from(template);
    }

    public void delete(Integer memberId, Integer templateId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        ChatQuickResponseTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Quick response not found.");
        }
        if (!sellerId.equals(template.getSellerId())) {
            throw new IllegalArgumentException("No permission to delete this quick response.");
        }
        templates.remove(templateId);
    }

    private void apply(ChatQuickResponseTemplate template, QuickResponseRequest request) {
        String title = request.title() == null ? "" : request.title().trim();
        String content = request.content() == null ? "" : request.content().trim();
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("名稱與回覆內容不可空白。");
        }
        template.setTitle(title);
        template.setContent(content);
    }
}
