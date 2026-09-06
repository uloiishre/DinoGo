package com.dinogo.chat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "chat", name = "Conversation",
        uniqueConstraints = @UniqueConstraint(name = "uq_chat_conversation_buyer_seller", columnNames = { "buyer_id", "seller_id" }),
        indexes = {
                @Index(name = "ix_chat_conversation_buyer_updated", columnList = "buyer_id, updated_at"),
                @Index(name = "ix_chat_conversation_seller_updated", columnList = "seller_id, updated_at")
        })
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Integer conversationId;

    @Column(name = "buyer_id", nullable = false)
    private Integer buyerId;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "buyer_unread_count", nullable = false)
    private Integer buyerUnreadCount = 0;

    @Column(name = "seller_unread_count", nullable = false)
    private Integer sellerUnreadCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private ChatMessage lastMessage;

    @Column(name = "latest_message_at")
    private LocalDateTime latestMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (buyerUnreadCount == null) buyerUnreadCount = 0;
        if (sellerUnreadCount == null) sellerUnreadCount = 0;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
