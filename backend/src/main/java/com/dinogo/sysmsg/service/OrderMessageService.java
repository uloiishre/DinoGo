package com.dinogo.sysmsg.service;

import java.util.List;

import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.request.auto.OrderEventRequest;

/** 只接受 orderId，實作透過 order 模組的 Provider Service 取得權威狀態與收件人。 */
public interface OrderMessageService {
    List<SendResponse> createOrderEventMessages(OrderEventRequest request);
}
