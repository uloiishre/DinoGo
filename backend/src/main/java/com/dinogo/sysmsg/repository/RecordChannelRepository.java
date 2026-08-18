package com.dinogo.sysmsg.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dinogo.sysmsg.entity.ChannelType;
import com.dinogo.sysmsg.entity.RecordChannelEntity;

public interface RecordChannelRepository extends JpaRepository<RecordChannelEntity, Integer> {
    Optional<RecordChannelEntity> findByRecordIdAndChannelType(
            Integer recordId, ChannelType channelType);

    List<RecordChannelEntity>
    findByChannelTypeAndSentAtIsNullAndErrorMessageIsNullOrderByRecordChannelId(
            ChannelType channelType);
}
