package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.RecordStatus;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;
import com.dinogo.sysmsg.repository.RecordRepository;
import com.dinogo.sysmsg.repository.SendRepository;
import com.dinogo.sysmsg.service.impl.RecordServiceImpl;
import com.dinogo.sysmsg.service.mapper.RecordResponseMapper;

/** 收件紀錄已讀與前綴刪除政策的正式單元測試。 */
class RecordLifecycleServiceTest {
    private RecordRepository records;
    private SendRepository sends;
    private RecordServiceImpl service;

    @BeforeEach
    void setUp() {
        records = mock(RecordRepository.class);
        sends = mock(SendRepository.class);
        service = new RecordServiceImpl(records, sends, mock(MsgPermissionService.class),
                mock(RecordChannelService.class), mock(MemberSysmsgProviderService.class),
                mock(SellerSysmsgProviderService.class), new RecordResponseMapper());
        when(records.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void openingUnreadRecordMarksItReadAndOpeningAgainIsIdempotent() {
        RecordEntity record = memberRecord("AC-001");
        when(records.findById(1)).thenReturn(Optional.of(record));

        service.readRecord(1, 7);
        service.readRecord(1, 7);

        assertEquals(RecordStatus.READ, record.getRecordStatus());
        verify(records, org.mockito.Mockito.times(2)).save(record);
    }

    @Test
    void systemRecordIsHardDeletedButOrderRecordIsSoftDeleted() {
        RecordEntity system = memberRecord("OA-001");
        RecordEntity order = memberRecord("AC-001");
        when(records.findById(1)).thenReturn(Optional.of(system));
        when(records.findById(2)).thenReturn(Optional.of(order));

        service.deleteRecord(1, 7);
        service.deleteRecord(2, 7);

        verify(records).delete(system);
        assertEquals(RecordStatus.DELETE, order.getRecordStatus());
        verify(records).save(order);
    }

    @Test
    void deletedRecordCannotBeReadAgain() {
        RecordEntity record = memberRecord("AC-001");
        record.setRecordStatus(RecordStatus.DELETE);
        when(records.findById(1)).thenReturn(Optional.of(record));

        assertThrows(IllegalStateException.class, () -> service.readRecord(1, 7));
        verify(records, never()).save(any());
    }

    @Test
    void recordCannotBeCreatedFromSaveTemplate() {
        SendEntity template = send("OC-001", SendStatus.SAVE);
        when(sends.findById(10)).thenReturn(Optional.of(template));

        assertThrows(IllegalStateException.class,
                () -> service.createRecords(10, java.util.List.of(7), java.util.List.of()));
        verify(records, never()).save(any());
    }

    private RecordEntity memberRecord(String function) {
        SendEntity send = send(function, SendStatus.SEND);
        RecordEntity record = new RecordEntity(send, 7, null);
        record.setRecordId(1);
        // 純單元測試不會觸發 JPA @PrePersist，明確模擬已寫入 DB 的初始狀態。
        record.setRecordStatus(RecordStatus.UNREAD);
        return record;
    }

    private SendEntity send(String function, SendStatus status) {
        SendEntity send = new SendEntity(1, function, "label", "title", "content", status);
        send.setSendId(10);
        return send;
    }
}
