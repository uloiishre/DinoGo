package com.dinogo.sysmsg.service;

import java.util.List;

import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.request.auto.OrderEventRequest;

/** 只接受 orderId，實作必須透過 OrderClient 取得權威狀態與收件人。 */
public interface OrderMessageService {
    List<SendResponse> createOrderEventMessages(OrderEventRequest request);
}
