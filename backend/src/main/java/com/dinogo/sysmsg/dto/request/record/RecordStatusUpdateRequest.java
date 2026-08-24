package com.dinogo.sysmsg.dto.request.record;

import com.dinogo.sysmsg.entity.RecordStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 閱讀訊息。
 *
 * 目前主要：
 *
 * UNREAD → READ
 *
 * 不允許由此 DTO 任意修改 DELETE。
 */
public class RecordStatusUpdateRequest {

    @NotNull
    private RecordStatus targetStatus;

    public RecordStatusUpdateRequest() {
    }

    public RecordStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(RecordStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
