package com.dinogo.chat.dto;

import java.time.LocalDateTime;

import com.dinogo.chat.entity.ChatQuickResponseTemplate;

public record QuickResponseResponse(
        Integer templateId,
        Integer sellerId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static QuickResponseResponse from(ChatQuickResponseTemplate template) {
        return new QuickResponseResponse(
                template.getTemplateId(),
                template.getSellerId(),
                template.getTitle(),
                template.getContent(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
