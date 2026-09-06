package com.dinogo.chat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dinogo.chat.dto.QuickResponseRequest;
import com.dinogo.chat.dto.QuickResponseResponse;
import com.dinogo.chat.entity.ChatQuickResponseTemplate;
import com.dinogo.chat.repository.ChatQuickResponseTemplateRepository;
import com.dinogo.seller.service.CurrentSellerService;

@Service
@Transactional(readOnly = true)
public class QuickResponseService {

    private final CurrentSellerService currentSellerService;
    private final ChatQuickResponseTemplateRepository templateRepository;

    public QuickResponseService(
            CurrentSellerService currentSellerService,
            ChatQuickResponseTemplateRepository templateRepository) {
        this.currentSellerService = currentSellerService;
        this.templateRepository = templateRepository;
    }

    public List<QuickResponseResponse> list(Integer memberId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        return templateRepository.findBySellerIdOrderByUpdatedAtDescTemplateIdDesc(sellerId).stream()
                .map(QuickResponseResponse::from)
                .toList();
    }

    @Transactional
    public QuickResponseResponse create(Integer memberId, QuickResponseRequest request) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        LocalDateTime now = LocalDateTime.now();
        ChatQuickResponseTemplate template = new ChatQuickResponseTemplate();
        template.setSellerId(sellerId);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        apply(template, request);
        return QuickResponseResponse.from(templateRepository.save(template));
    }

    @Transactional
    public QuickResponseResponse update(Integer memberId, Integer templateId, QuickResponseRequest request) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        ChatQuickResponseTemplate template = requireSellerTemplate(templateId, sellerId);
        apply(template, request);
        template.setUpdatedAt(LocalDateTime.now());
        return QuickResponseResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void delete(Integer memberId, Integer templateId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        ChatQuickResponseTemplate template = requireSellerTemplate(templateId, sellerId);
        templateRepository.delete(template);
    }

    private ChatQuickResponseTemplate requireSellerTemplate(Integer templateId, Integer sellerId) {
        return templateRepository.findByTemplateIdAndSellerId(templateId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Quick response not found."));
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
