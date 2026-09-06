package com.dinogo.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.chat.entity.ChatQuickResponseTemplate;

public interface ChatQuickResponseTemplateRepository extends JpaRepository<ChatQuickResponseTemplate, Integer> {

    List<ChatQuickResponseTemplate> findBySellerIdOrderByUpdatedAtDescTemplateIdDesc(Integer sellerId);

    Optional<ChatQuickResponseTemplate> findByTemplateIdAndSellerId(Integer templateId, Integer sellerId);
}
