package com.dinogo.sysmsg.service.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.dinogo.sysmsg.entity.*;
import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.dto.request.record.SendStatusUpdateRequest;
import com.dinogo.sysmsg.dto.request.send.*;
import com.dinogo.sysmsg.dto.request.template.*;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.client.*;
import com.dinogo.sysmsg.repository.*;
import com.dinogo.sysmsg.service.*;

/** 範本、手動發訊與 Send 狀態的交易邊界。 */
@Service
public class SendServiceImpl implements SendService {
    private final SendRepository sends;
    private final RecordService records;
    private final TemplateNumService numbers;
    private final MsgPermissionService permissions;
    private final OrderClient orders;
    private final MemberClient members;
    private final SellerClient sellers;
    private final ApplicationEventPublisher events;

    public SendServiceImpl(SendRepository sends, RecordService records, TemplateNumService numbers,
            MsgPermissionService permissions, OrderClient orders, MemberClient members, SellerClient sellers,
            ApplicationEventPublisher events) {
        this.sends=sends; this.records=records; this.numbers=numbers; this.permissions=permissions; this.orders=orders;
        this.members=members; this.sellers=sellers;
        this.events=events;
    }

    @Override @Transactional
    public SendTemplateResponse createSellerTemplate(SellerTemplateCreateRequest r, Integer loginId) {
        Integer sellerId=permissions.validateSeller(loginId);
        SendSellerEntity entity=new SendSellerEntity(sellerId,numbers.generateMsgFunction("SC"),label(r.getMsgLabel(),r.getSendTitle()),
                required(r.getSendTitle(),"標題"),required(r.getSendContent(),"內容"),SendStatus.SAVE,
                r.getImgOne(),r.getImgTwo(),r.getImgThree(),r.getSendRemark());
        return template(sends.save(entity));
    }

    @Override @Transactional
    public SendTemplateResponse createSystemTemplate(SysTemplateCreateRequest r, Integer loginId) {
        permissions.validateSystemAdmin(loginId); String prefix=systemPrefix(r.getMsgType());
        return template(sends.save(base(1,numbers.generateMsgFunction(prefix),r.getMsgLabel(),r.getSendTitle(),r.getSendContent(),SendStatus.SAVE)));
    }

    @Override @Transactional
    public SendTemplateResponse updateTemplate(Integer sendId, SendTemplateUpdateRequest r, Integer loginId) {
        SendEntity old=require(sendId); requireStatus(old,SendStatus.SAVE); permissions.validateTemplateOwner(old,loginId);
        if (!sends.existsByMsgFunctionAndSendStatus(old.getMsgFunction(),SendStatus.SEND)) {
            old.setMsgLabel(label(r.getMsgLabel(),r.getSendTitle())); old.setSendTitle(required(r.getSendTitle(),"標題")); old.setSendContent(required(r.getSendContent(),"內容"));
            if(old instanceof SendSellerEntity sc){sc.setImgOne(r.getImgOne());sc.setImgTwo(r.getImgTwo());sc.setImgThree(r.getImgThree());sc.setSendRemark(r.getSendRemark());}
            return template(sends.save(old));
        }
        SendEntity replacement;
        String function=numbers.generateMsgFunction(prefix(old));
        if(old instanceof SendSellerEntity) replacement=new SendSellerEntity(old.getMsgfromSellerId(),function,label(r.getMsgLabel(),r.getSendTitle()),r.getSendTitle(),r.getSendContent(),SendStatus.SAVE,r.getImgOne(),r.getImgTwo(),r.getImgThree(),r.getSendRemark());
        else replacement=base(old.getMsgfromSellerId(),function,r.getMsgLabel(),r.getSendTitle(),r.getSendContent(),SendStatus.SAVE);
        sends.delete(old); return template(sends.save(replacement));
    }

    @Override @Transactional
    public SendResponse applySystemTemplate(SysTemplateApplyRequest r,Integer loginId){
        SendEntity t=require(r.getSendId()); requireStatus(t,SendStatus.SAVE); permissions.validateTemplateOwner(t,loginId);
        String p=systemPrefix(prefix(t)); SendEntity actual=copy(t,t.getMsgFunction(),SendStatus.SEND); actual=sends.save(actual);
        createSystemRecords(actual,p,r.getMsgtoMemberId(),r.getMsgtoSellerId()); return response(actual);
    }

    @Override @Transactional
    public SendResponse applySellerTemplate(SellerTemplateApplyRequest r,Integer loginId){
        Integer sellerId=permissions.validateSeller(loginId); SendEntity t=require(r.getSendId()); requireStatus(t,SendStatus.SAVE); permissions.validateTemplateOwner(t,loginId);
        if(!(t instanceof SendSellerEntity sc)||!"SC".equals(prefix(t)))throw new IllegalStateException("只能套用 SC 範本");
        OrderInfoResponse order=validOrder(orders.getOrder(r.getOrderId())); requireOrderOwner(order,sellerId);
        SendSellerEntity actual=new SendSellerEntity(sellerId,t.getMsgFunction(),t.getMsgLabel(),t.getSendTitle(),t.getSendContent(),SendStatus.SEND,sc.getImgOne(),sc.getImgTwo(),sc.getImgThree(),sc.getSendRemark());
        sends.save(actual); records.createSingleMemberRecord(actual.getSendId(),order.getBuyerId()); return response(actual);
    }

    @Override @Transactional
    public SendResponse createSellerSend(SellerCreateRequest r,Integer loginId){
        Integer sellerId=permissions.validateSeller(loginId); OrderInfoResponse order=validOrder(orders.getOrder(r.getOrderId())); requireOrderOwner(order,sellerId);
        SendSellerEntity actual=new SendSellerEntity(sellerId,numbers.generateMsgFunction("SC"),r.getSendTitle(),r.getSendTitle(),r.getSendContent(),SendStatus.SEND,r.getImgOne(),r.getImgTwo(),r.getImgThree(),r.getSendRemark());
        sends.save(actual); records.createSingleMemberRecord(actual.getSendId(),order.getBuyerId()); return response(actual);
    }

    @Override @Transactional
    public SendResponse createSystemSend(SysCreateRequest r,Integer loginId){
        permissions.validateSystemAdmin(loginId); String p=systemPrefix(r.getMsgType());
        SendEntity actual=sends.save(base(1,numbers.generateMsgFunction(p),null,r.getSendTitle(),r.getSendContent(),SendStatus.SEND));
        createSystemRecords(actual,p,r.getMsgtoMemberId(),r.getMsgtoSellerId()); return response(actual);
    }

    @Override @Transactional
    public SendResponse changeSendStatus(Integer id,SendStatusUpdateRequest r,Integer loginId){
        SendEntity s=require(id); permissions.validateTemplateOwner(s,loginId); SendStatus target=Objects.requireNonNull(r.getTargetStatus(),"targetStatus");
        boolean legal=(s.getSendStatus()==SendStatus.SAVE&&(target==SendStatus.SEND||target==SendStatus.DELETE))||(s.getSendStatus()==SendStatus.SEND&&target==SendStatus.DELETE);
        if(!legal)throw new IllegalStateException("不合法的 Send 狀態轉換："+s.getSendStatus()+" → "+target);
        if(target==SendStatus.SEND&&records.existsBySendId(id))throw new IllegalStateException("SAVE 範本本身不能成為已存在 Record 的 SEND");
        s.setSendStatus(target); return response(sends.save(s));
    }
    @Override @Transactional public void deleteSend(Integer id,Integer loginId){ SendStatusUpdateRequest r=new SendStatusUpdateRequest();r.setTargetStatus(SendStatus.DELETE);changeSendStatus(id,r,loginId); }
    @Override @Transactional(readOnly=true) public List<SendTemplateResponse> findTemplates(Integer loginId){
        Integer sellerId; try{sellerId=permissions.validateSeller(loginId);}catch(SecurityException e){permissions.validateSystemAdmin(loginId);sellerId=1;}
        return sends.findByMsgfromSellerIdAndSendStatusOrderBySendUpdAtDesc(sellerId,SendStatus.SAVE).stream().map(this::template).toList();
    }

    @Override @Transactional(readOnly=true) public List<SendResponse> findSellerOutbox(Integer loginId){
        Integer sellerId=permissions.validateSeller(loginId);
        return sends.findByMsgfromSellerIdAndSendStatusOrderBySendUpdAtDesc(sellerId,SendStatus.SEND).stream().map(this::response).toList();
    }

    private SendEntity base(Integer from,String fn,String lbl,String title,String content,SendStatus status){return new SendEntity(from,fn,label(lbl,title),required(title,"標題"),required(content,"內容"),status);}
    private SendEntity copy(SendEntity t,String fn,SendStatus status){return base(t.getMsgfromSellerId(),fn,t.getMsgLabel(),t.getSendTitle(),t.getSendContent(),status);}
    private SendEntity require(Integer id){return sends.findById(id).orElseThrow(()->new IllegalArgumentException("找不到 Send："+id));}
    private void requireStatus(SendEntity s,SendStatus status){if(s.getSendStatus()!=status)throw new IllegalStateException("Send 必須為 "+status);}
    private String prefix(SendEntity s){return s.getMsgFunction().substring(0,2);}
    private String systemPrefix(String p){String x=required(p,"msgType").trim().toUpperCase();if(!Set.of("OA","OC","OS").contains(x))throw new IllegalArgumentException("系統訊息僅允許 OA/OC/OS");return x;}
    private String label(String value,String title){return value==null||value.isBlank()?required(title,"標題"):value.trim();}
    private String required(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+"不可空白");return value.trim();}
    private void createSystemRecords(SendEntity send,String p,Integer memberId,Integer sellerId){
        if("OA".equals(p)){events.publishEvent(new OaBroadcastRequested(send.getSendId()));return;}
        records.createRecords(send.getSendId(),memberRecipients(p,memberId),sellerRecipients(p,sellerId));
    }
    private List<Integer> memberRecipients(String p,Integer id){
        if("OS".equals(p))return List.of();
        if("OA".equals(p))return List.of();
        if(id==null)throw new IllegalArgumentException("OC 必須提供會員收件人");
        members.getMember(id); return List.of(id);
    }
    private List<Integer> sellerRecipients(String p,Integer id){
        if("OC".equals(p))return List.of();
        if("OA".equals(p))return List.of();
        if(id==null)throw new IllegalArgumentException("OS 必須提供商家收件人");
        if(!sellers.getSeller(id).isActive())throw new IllegalStateException("收件商家未啟用："+id);
        return List.of(id);
    }
    private OrderInfoResponse validOrder(OrderInfoResponse o){if(o==null||o.getOrderId()==null||o.getBuyerId()==null||o.getSellerId()==null)throw new IllegalStateException("Order API 回傳資料不完整");return o;}
    private void requireOrderOwner(OrderInfoResponse o,Integer sellerId){if(!sellerId.equals(o.getSellerId()))throw new SecurityException("該商家無權使用此訂單");}
    private SendResponse response(SendEntity s){SendResponse x=new SendResponse();x.setSendId(s.getSendId());x.setMsgFunction(s.getMsgFunction());x.setMsgfromSellerId(s.getMsgfromSellerId());x.setMsgLabel(s.getMsgLabel());x.setSendTitle(s.getSendTitle());x.setSendContent(s.getSendContent());x.setSendStatus(s.getSendStatus());x.setSendUpdAt(s.getSendUpdAt());return x;}
    private SendTemplateResponse template(SendEntity s){SendTemplateResponse x=new SendTemplateResponse();x.setSendId(s.getSendId());x.setMsgFunction(s.getMsgFunction());x.setMsgfromSellerId(s.getMsgfromSellerId());x.setMsgLabel(s.getMsgLabel());x.setSendTitle(s.getSendTitle());x.setSendContent(s.getSendContent());x.setSendStatus(s.getSendStatus());x.setSendUpdAt(s.getSendUpdAt());return x;}
}
