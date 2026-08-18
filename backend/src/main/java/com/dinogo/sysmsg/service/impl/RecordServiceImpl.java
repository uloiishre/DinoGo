package com.dinogo.sysmsg.service.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dinogo.sysmsg.entity.*;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.dto.external.*;
import com.dinogo.sysmsg.client.*;
import com.dinogo.sysmsg.repository.*;
import com.dinogo.sysmsg.service.*;

@Service
public class RecordServiceImpl implements RecordService {
    private final RecordRepository records;
    private final SendRepository sends;
    private final MsgPermissionService permissions;
    private final RecordChannelService channels;
    private final MemberClient members;
    private final SellerClient sellers;

    public RecordServiceImpl(RecordRepository records, SendRepository sends, MsgPermissionService permissions,
            RecordChannelService channels, MemberClient members, SellerClient sellers) {
        this.records = records; this.sends = sends; this.permissions = permissions;
        this.channels=channels; this.members=members; this.sellers=sellers;
    }

    @Override @Transactional
    public List<RecordResponse> createRecords(Integer sendId, List<Integer> memberIds, List<Integer> sellerIds) {
        SendEntity send = requireSend(sendId);
        if (send.getSendStatus() != SendStatus.SEND) throw new IllegalStateException("Record 只能指向 send_status=SEND：" + sendId);
        List<RecordResponse> result = new ArrayList<>();
        for (Integer id : distinct(memberIds)) {
            requireRecipientType(send, false);
            MemberAuthResponse member=members.getMember(positive(id, "member_id"));
            RecordEntity record = new RecordEntity(send, positive(id, "member_id"), null);
            record=records.save(record); channels.createEmailChannel(sendId,record.getRecordId(),member,send.getMsgFunction(),false);
            result.add(toResponse(record));
        }
        for (Integer id : distinct(sellerIds)) {
            requireRecipientType(send, true);
            SellerInfoResponse seller=sellers.getSeller(positive(id, "seller_id"));
            if(!seller.isActive())throw new IllegalStateException("收件商家未啟用："+id);
            RecordEntity record = new RecordEntity(send, null, positive(id, "seller_id"));
            record=records.save(record); channels.createEmailChannel(sendId,record.getRecordId(),asMemberProfile(seller),send.getMsgFunction(),true);
            result.add(toResponse(record));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("至少需要一位收件人");
        return result;
    }

    /** 只供訂單通知建立帶快照的 Record；memberId、sellerId 必須二選一。 */
    @Override @Transactional
    public RecordResponse createOrderRecord(Integer sendId, Integer memberId, Integer sellerId, Integer orderId, String status) {
        SendEntity send = requireSend(sendId);
        if (send.getSendStatus() != SendStatus.SEND) throw new IllegalStateException("Send 尚未發送");
        RecordEntity record = new RecordEntity(send, memberId, sellerId);
        record.setOrderId(positive(orderId, "order_id")); record.setOrderStatus(status);
        record=records.save(record);
        if(memberId!=null)channels.createEmailChannel(sendId,record.getRecordId(),members.getMember(memberId),send.getMsgFunction(),false);
        else channels.createEmailChannel(sendId,record.getRecordId(),asMemberProfile(sellers.getSeller(sellerId)),send.getMsgFunction(),true);
        return toResponse(record);
    }

    @Override @Transactional public RecordResponse createSingleMemberRecord(Integer sendId, Integer memberId) {
        return createRecords(sendId, List.of(memberId), List.of()).get(0);
    }
    @Override @Transactional public RecordResponse createSingleSellerRecord(Integer sendId, Integer sellerId) {
        return createRecords(sendId, List.of(), List.of(sellerId)).get(0);
    }
    @Override @Transactional(readOnly=true) public boolean existsBySendId(Integer sendId) { return records.existsBySend_SendId(sendId); }
    @Override @Transactional(readOnly=true) public RecordResponse searchRecord(Integer id, Integer loginId) {
        RecordEntity r = requireRecord(id); permissions.validateRecordOwner(r, loginId); return toResponse(r);
    }
    @Override @Transactional public RecordResponse readRecord(Integer id, Integer loginId) {
        RecordEntity r = requireRecord(id); permissions.validateRecordOwner(r, loginId);
        if (r.getRecordStatus() == RecordStatus.DELETE) throw new IllegalStateException("DELETE 訊息不能閱讀");
        if (r.getRecordStatus() == RecordStatus.UNREAD) r.setRecordStatus(RecordStatus.READ);
        return toResponse(records.save(r));
    }
    @Override @Transactional public void deleteRecord(Integer id, Integer loginId) {
        RecordEntity r = requireRecord(id); permissions.validateRecordOwner(r, loginId);
        if (r.getRecordStatus() == RecordStatus.DELETE) throw new IllegalStateException("訊息已刪除");
        String p = prefix(r); if (Set.of("OA","OC","OS").contains(p)) records.delete(r);
        else { r.setRecordStatus(RecordStatus.DELETE); records.save(r); }
    }
    @Override @Transactional(readOnly=true) public List<MsgInboxResponse> getMemberInbox(Integer id, String inbox) {
        MemberInbox wanted = MemberInbox.valueOf(inbox.trim().toUpperCase());
        return records.findByMsgtoMemberIdAndRecordStatusNotOrderByRecordCreatedAtDesc(id, RecordStatus.DELETE)
                .stream().filter(r -> memberInbox(r) == wanted).map(this::toInbox).toList();
    }
    @Override @Transactional(readOnly=true) public List<MsgInboxResponse> getSellerInbox(Integer id, String inbox) {
        SellerInbox wanted = SellerInbox.valueOf(inbox.trim().toUpperCase());
        return records.findByMsgtoSellerIdAndRecordStatusNotOrderByRecordCreatedAtDesc(id, RecordStatus.DELETE)
                .stream().filter(r -> sellerInbox(r) == wanted).map(this::toInbox).toList();
    }

    private SendEntity requireSend(Integer id) { return sends.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到 Send：" + id)); }
    private RecordEntity requireRecord(Integer id) { return records.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到 Record：" + id)); }
    private Integer positive(Integer id, String name) { if (id == null || id <= 0) throw new IllegalArgumentException(name + " 必須是正整數"); return id; }
    private List<Integer> distinct(List<Integer> ids) { return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList(); }
    private String prefix(RecordEntity r) { return r.getMsgFunction().substring(0, 2); }
    private void requireRecipientType(SendEntity send,boolean seller){
        String p=send.getMsgFunction().substring(0,2);
        boolean allowed=seller?Set.of("OA","OS","AS").contains(p):Set.of("OA","OC","AC","SC").contains(p);
        if(!allowed)throw new IllegalArgumentException(p+" 不允許此收件人類型");
    }
    private MemberAuthResponse asMemberProfile(SellerInfoResponse seller){
        MemberAuthResponse x=new MemberAuthResponse();x.setMemberId(seller.getMemberId());x.setSellerId(seller.getSellerId());x.setEmail(seller.getEmail());
        x.setEmailOrderNotifications(seller.getEmailOrderNotifications());x.setEmailMarketingNotifications(seller.getEmailMarketingNotifications());x.setAuthenticated(true);return x;
    }
    private RecordResponse toResponse(RecordEntity r) {
        RecordResponse x=new RecordResponse(); x.setRecordId(r.getRecordId()); x.setSendId(r.getSend().getSendId()); x.setMsgFunction(r.getMsgFunction());
        x.setMsgfromSellerId(r.getMsgfromSellerId()); x.setMsgtoMemberId(r.getMsgtoMemberId()); x.setMsgtoSellerId(r.getMsgtoSellerId());
        x.setOrderId(r.getOrderId()); x.setOrderStatus(r.getOrderStatus()); x.setRecordStatus(r.getRecordStatus()); x.setRecordCreatedAt(r.getRecordCreatedAt()); return x;
    }
    private MsgInboxResponse toInbox(RecordEntity r) {
        SendEntity s=r.getSend(); MsgInboxResponse x=new MsgInboxResponse(); x.setRecordId(r.getRecordId()); x.setSendId(s.getSendId()); x.setMsgFunction(r.getMsgFunction());
        x.setMsgfromSellerId(r.getMsgfromSellerId()); x.setMsgtoMemberId(r.getMsgtoMemberId()); x.setMsgtoSellerId(r.getMsgtoSellerId()); x.setOrderId(r.getOrderId()); x.setOrderStatus(r.getOrderStatus());
        x.setMsgLabel(s.getMsgLabel()); x.setSendTitle(s.getSendTitle()); x.setSendContent(s.getSendContent()); x.setRecordStatus(r.getRecordStatus()); x.setRecordCreatedAt(r.getRecordCreatedAt());
        x.setMemberInbox(memberInbox(r)); x.setSellerInbox(sellerInbox(r)); return x;
    }
    private MemberInbox memberInbox(RecordEntity r) { if(r.getMsgtoMemberId()==null)return null; return switch(prefix(r)){case "OA","OC"->MemberInbox.SYSTEM_INBOX;case "AC"->MemberInbox.ORDER_INBOX;case "SC"->MemberInbox.SELLER_INBOX;default->null;}; }
    private SellerInbox sellerInbox(RecordEntity r) { if(r.getMsgtoSellerId()==null)return null; String p=prefix(r); if(Set.of("OA","OS").contains(p))return SellerInbox.SYSTEM_NOTICE; return "AS".equals(p)&&"CANCELLED".equals(r.getOrderStatus())?SellerInbox.CANCELLED_ORDER:"AS".equals(p)?SellerInbox.NEW_ORDER:null; }
}
