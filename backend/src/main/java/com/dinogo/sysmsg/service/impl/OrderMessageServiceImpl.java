package com.dinogo.sysmsg.service.impl;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dinogo.sysmsg.entity.*;
import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.dto.request.auto.*;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.client.OrderClient;
import com.dinogo.sysmsg.repository.*;
import com.dinogo.sysmsg.service.*;

@Service
public class OrderMessageServiceImpl implements OrderMessageService {
    private static final Set<String> CUSTOMER = Set.of("PAID","SHIPPED","DELIVERED","COMPLETED");
    private static final Set<String> SELLER = Set.of("PAID","SHIPPED","COMPLETED");
    private static final Map<String,String> CUSTOMER_TITLE = Map.of("PAID","訂單付款成功","SHIPPED","訂單已出貨","DELIVERED","訂單已到貨","COMPLETED","訂單已完成");
    private static final Map<String,String> SELLER_TITLE = Map.of("PAID","收到新訂單","SHIPPED","訂單已出貨","COMPLETED","訂單完成與收款");
    private final OrderClient orders; private final SendRepository sends; private final RecordRepository records;
    private final RecordService recordService; private final TemplateNumService numbers;
    public OrderMessageServiceImpl(OrderClient orders,SendRepository sends,RecordRepository records,RecordService recordService,TemplateNumService numbers){this.orders=orders;this.sends=sends;this.records=records;this.recordService=recordService;this.numbers=numbers;}

    @Override @Transactional
    public List<SendResponse> createOrderEventMessages(OrderEventRequest r){
        OrderInfoResponse order=require(orders.getOrder(r.getOrderId())); String status=normalize(order.getStatus());
        List<SendResponse> result=new ArrayList<>();
        if("CANCELLED".equals(status)){
            result.add(cancelledFromOrder(order,true)); result.add(cancelledFromOrder(order,false)); return result;
        }
        if(!CUSTOMER.contains(status))throw new IllegalStateException("不支援訂單狀態："+status);
        result.add(normalFromOrder(order,true));
        if(SELLER.contains(status))result.add(normalFromOrder(order,false));
        return result;
    }
    private SendResponse normalFromOrder(OrderInfoResponse o,boolean customer){
        String status=normalize(o.getStatus());
        if(!(customer?CUSTOMER:SELLER).contains(status))throw new IllegalStateException((customer?"AC":"AS")+" 不支援訂單狀態："+status);
        rejectDuplicate(o,status,customer); String prefix=customer?"AC":"AS"; String title=(customer?CUSTOMER_TITLE:SELLER_TITLE).get(status);
        String content=content(o,status,customer);
        SendOrderEntity send=new SendOrderEntity(1,numbers.generateMsgFunction(prefix),o.getOrderNo(),title,content,SendStatus.SEND,o.getOrderId(),o.getOrderNo(),o.getTotalAmount(),o.getPaymentMethodId(),o.getMethodName(),o.getCreatedAt(),status);
        sends.save(send); recordService.createOrderRecord(send.getSendId(),customer?o.getBuyerId():null,customer?null:o.getSellerId(),o.getOrderId(),status); return response(send);
    }
    private SendResponse cancelledFromOrder(OrderInfoResponse o,boolean customer){
        String status=normalize(o.getStatus());if(!"CANCELLED".equals(status))throw new IllegalStateException("訂單不是 CANCELLED");rejectDuplicate(o,status,customer);
        String prefix=customer?"AC":"AS";String title="訂單已取消";String reason=o.getCancelReason()==null?"未提供原因":o.getCancelReason();
        SendDisorderEntity send=new SendDisorderEntity(1,numbers.generateMsgFunction(prefix),o.getOrderNo(),title,"訂單 "+o.getOrderNo()+" 已取消，原因："+reason+"。",SendStatus.SEND,o.getOrderId(),o.getOrderNo(),o.getTotalAmount(),o.getPaymentMethodId(),o.getMethodName(),o.getCancelReason(),o.getCancelledAt(),status);
        sends.save(send);recordService.createOrderRecord(send.getSendId(),customer?o.getBuyerId():null,customer?null:o.getSellerId(),o.getOrderId(),status);return response(send);
    }
    private void rejectDuplicate(OrderInfoResponse o,String status,boolean customer){boolean exists=customer?records.existsByOrderIdAndOrderStatusAndMsgtoMemberId(o.getOrderId(),status,o.getBuyerId()):records.existsByOrderIdAndOrderStatusAndMsgtoSellerId(o.getOrderId(),status,o.getSellerId());if(exists)throw new IllegalStateException("訂單通知已存在："+o.getOrderId()+"/"+status);}
    private OrderInfoResponse require(OrderInfoResponse o){
        if(o==null||o.getOrderId()==null||o.getBuyerId()==null||o.getSellerId()==null||o.getOrderNo()==null
                ||o.getTotalAmount()==null||o.getPaymentMethodId()==null||o.getMethodName()==null)
            throw new IllegalStateException("Order API 回傳資料不完整");
        return o;
    }
    private String normalize(String s){if(s==null)throw new IllegalStateException("Order API 未回傳狀態");return s.trim().toUpperCase();}
    private String content(OrderInfoResponse o,String s,boolean customer){return switch(s){case "PAID"->customer?"訂單 "+o.getOrderNo()+" 已付款成功。":"收到訂單 "+o.getOrderNo()+"，請開始處理。";case "SHIPPED"->"訂單 "+o.getOrderNo()+" 已出貨。";case "DELIVERED"->"訂單 "+o.getOrderNo()+" 已送達。";case "COMPLETED"->customer?"訂單 "+o.getOrderNo()+" 已完成。":"訂單 "+o.getOrderNo()+" 已完成，收入金額為 "+o.getTotalAmount()+"。";default->throw new IllegalStateException();};}
    private SendResponse response(SendEntity s){SendResponse x=new SendResponse();x.setSendId(s.getSendId());x.setMsgFunction(s.getMsgFunction());x.setMsgfromSellerId(s.getMsgfromSellerId());x.setMsgLabel(s.getMsgLabel());x.setSendTitle(s.getSendTitle());x.setSendContent(s.getSendContent());x.setSendStatus(s.getSendStatus());x.setSendUpdAt(s.getSendUpdAt());return x;}
}
