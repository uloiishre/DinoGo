package com.dinogo.sysmsg.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Nationalized;
import jakarta.persistence.*;

@Entity
@Table(name = "record_channel", schema = "sysmsg",
       uniqueConstraints = @UniqueConstraint(
           name = "UX_sysmsg_record_channel_type",
           columnNames = {"record_id", "channel_type"}))
public class RecordChannelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_channel_id")
    private Integer recordChannelId;

    @Column(name = "send_id", nullable = false, updatable = false)
    private Integer sendId;

    @Column(name = "record_id", nullable = false, updatable = false)
    private Integer recordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 10, nullable = false, updatable = false)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20, nullable = false, updatable = false)
    private NotificationType notificationType;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Nationalized
    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;
    @Nationalized
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected RecordChannelEntity() {}

    public RecordChannelEntity(Integer sendId, Integer recordId,
            ChannelType channelType, NotificationType notificationType) {
        this.sendId = sendId;
        this.recordId = recordId;
        this.channelType = channelType;
        this.notificationType = notificationType;
    }

    public Integer getRecordChannelId() { return recordChannelId; }
    public Integer getSendId() { return sendId; }
    public Integer getRecordId() { return recordId; }
    public ChannelType getChannelType() { return channelType; }
    public NotificationType getNotificationType() { return notificationType; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getErrorMessage() { return errorMessage; }

    /** Spring Boot 完成送交 Email Provider 時呼叫；時間由應用程式明確控制。 */
    public void markSent(String providerMessageId, LocalDateTime sentAt) {
        if (sentAt == null) {
            throw new IllegalArgumentException("sentAt 不可為 null");
        }
        if (providerMessageId == null || providerMessageId.isBlank()) {
            throw new IllegalArgumentException("發送成功時必須保存 providerMessageId");
        }
        this.sentAt = sentAt;
        this.providerMessageId = providerMessageId;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("寄送失敗時必須提供 errorMessage");
        }
        this.sentAt = null;
        this.providerMessageId = null;
        this.errorMessage = errorMessage;
    }

    public boolean isPending() { return sentAt == null && errorMessage == null; }
    public boolean isSent() { return sentAt != null && errorMessage == null; }
    public boolean isFailed() { return sentAt == null && errorMessage != null; }
}
