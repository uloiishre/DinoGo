package com.dinogo.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.chat.entity.ChatConversation;

import jakarta.persistence.LockModeType;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT conversation
            FROM ChatConversation conversation
            WHERE conversation.buyerId = :buyerId
              AND conversation.sellerId = :sellerId
            """)
    Optional<ChatConversation> findByBuyerIdAndSellerIdForUpdate(
            @Param("buyerId") Integer buyerId,
            @Param("sellerId") Integer sellerId);

    @EntityGraph(attributePaths = "lastMessage")
    List<ChatConversation> findByBuyerIdOrderByUpdatedAtDescConversationIdDesc(Integer buyerId);

    @EntityGraph(attributePaths = "lastMessage")
    List<ChatConversation> findBySellerIdOrderByUpdatedAtDescConversationIdDesc(Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "lastMessage")
    @Query("SELECT conversation FROM ChatConversation conversation WHERE conversation.conversationId = :conversationId")
    Optional<ChatConversation> findByConversationIdForUpdate(@Param("conversationId") Integer conversationId);
}
