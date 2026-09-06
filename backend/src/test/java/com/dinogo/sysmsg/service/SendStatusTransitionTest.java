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
import org.springframework.context.ApplicationEventPublisher;

import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.dto.request.record.SendStatusUpdateRequest;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;
import com.dinogo.sysmsg.repository.SendRepository;
import com.dinogo.sysmsg.service.impl.SendServiceImpl;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;

/** SEND/SAVE/DELETE 狀態邊界的正式單元測試。 */
class SendStatusTransitionTest {
    private SendRepository sends;
    private RecordService records;
    private SendServiceImpl service;

    @BeforeEach
    void setUp() {
        sends = mock(SendRepository.class);
        records = mock(RecordService.class);
        service = new SendServiceImpl(sends, records, mock(TemplateNumService.class),
                mock(MsgPermissionService.class), mock(OrderSysmsgProviderService.class),
                mock(MemberSysmsgProviderService.class), mock(SellerSysmsgProviderService.class),
                mock(ApplicationEventPublisher.class), new SendResponseMapper(),
                mock(SysmsgImageService.class));
        when(sends.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveCanBecomeSendWhenNoRecordExists() {
        SendEntity template = send(SendStatus.SAVE);
        when(sends.findById(10)).thenReturn(Optional.of(template));

        service.changeSendStatus(10, request(SendStatus.SEND), 3);

        assertEquals(SendStatus.SEND, template.getSendStatus());
        verify(sends).save(template);
    }

    @Test
    void sendCannotReturnToSave() {
        SendEntity sent = send(SendStatus.SEND);
        when(sends.findById(10)).thenReturn(Optional.of(sent));

        assertThrows(IllegalStateException.class,
                () -> service.changeSendStatus(10, request(SendStatus.SAVE), 3));
        verify(sends, never()).save(any());
    }

    @Test
    void saveTemplateWithExistingRecordCannotBecomeSend() {
        SendEntity template = send(SendStatus.SAVE);
        when(sends.findById(10)).thenReturn(Optional.of(template));
        when(records.existsBySendId(10)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.changeSendStatus(10, request(SendStatus.SEND), 3));
        verify(sends, never()).save(any());
    }

    private SendEntity send(SendStatus status) {
        SendEntity send = new SendEntity(1, "OA-001", "label", "title", "content", status);
        send.setSendId(10);
        return send;
    }

    private SendStatusUpdateRequest request(SendStatus target) {
        SendStatusUpdateRequest request = new SendStatusUpdateRequest();
        request.setTargetStatus(target);
        return request;
    }
}
