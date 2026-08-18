package com.dinogo.sysmsg.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dinogo.sysmsg.entity.ChannelType;
import com.dinogo.sysmsg.entity.NotificationType;
import com.dinogo.sysmsg.entity.RecordChannelEntity;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.sysmsg.repository.RecordChannelRepository;
import com.dinogo.sysmsg.service.RecordChannelService;

@Service
public class RecordChannelServiceImpl implements RecordChannelService {
    private final RecordChannelRepository channelRepository;

    public RecordChannelServiceImpl(RecordChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    @Transactional
    public RecordChannelEntity createEmailChannel(
            Integer sendId, Integer recordId,
            MemberAuthResponse member, String msgFunction,
            boolean sellerRecipient) {
        NotificationType type = NotificationType.resolve(msgFunction, sellerRecipient);
        if (member == null || member.getEmail() == null || member.getEmail().isBlank()) {
            return null;
        }
        boolean enabled = type == NotificationType.ORDER
                ? Boolean.TRUE.equals(member.getEmailOrderNotifications())
                : Boolean.TRUE.equals(member.getEmailMarketingNotifications());
        if (!enabled) {
            return null;
        }
        if (channelRepository.findByRecordIdAndChannelType(
                recordId, ChannelType.EMAIL).isPresent()) {
            throw new IllegalStateException("此 Record 已建立 EMAIL 渠道：" + recordId);
        }
        return channelRepository.save(new RecordChannelEntity(
                sendId, recordId, ChannelType.EMAIL, type));
    }

    @Override
    @Transactional
    public RecordChannelEntity markEmailSent(Integer id, String providerMessageId, LocalDateTime sentAt) {
        RecordChannelEntity channel = requireEmail(id);
        channel.markSent(providerMessageId, sentAt);
        return channelRepository.save(channel);
    }

    @Override
    @Transactional
    public RecordChannelEntity markEmailFailed(Integer id, String errorMessage) {
        RecordChannelEntity channel = requireEmail(id);
        channel.markFailed(errorMessage);
        return channelRepository.save(channel);
    }

    private RecordChannelEntity requireEmail(Integer id) {
        RecordChannelEntity channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到 RecordChannel：" + id));
        if (channel.getChannelType() != ChannelType.EMAIL) {
            throw new IllegalArgumentException("目前只支援 EMAIL Channel");
        }
        return channel;
    }
}
