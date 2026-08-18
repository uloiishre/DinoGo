package com.dinogo.sysmsg.service;

import java.util.List;

import com.dinogo.sysmsg.dto.response.MsgInboxResponse;
import com.dinogo.sysmsg.dto.response.RecordResponse;

public interface RecordService {

    /**
     * 建立 Record。
     *
     * Spring Boot：
     *     從 send_id 取得 msg_function。
     */
    List<RecordResponse> createRecords(
            Integer sendId,
            List<Integer> memberIds,
            List<Integer> sellerIds
    );

    RecordResponse createSingleMemberRecord(
            Integer sendId,
            Integer memberId
    );

    RecordResponse createSingleSellerRecord(
            Integer sendId,
            Integer sellerId
    );

    RecordResponse createOrderRecord(Integer sendId, Integer memberId, Integer sellerId,
            Integer orderId, String orderStatus);

    boolean existsBySendId(Integer sendId);

    RecordResponse searchRecord(
            Integer recordId,
            Integer loginMemberId
    );

    RecordResponse readRecord(
            Integer recordId,
            Integer loginMemberId
    );

    void deleteRecord(
            Integer recordId,
            Integer loginMemberId
    );

    List<MsgInboxResponse> getMemberInbox(
            Integer memberId,
            String inbox
    );

    List<MsgInboxResponse> getSellerInbox(
            Integer sellerId,
            String inbox
    );
}
