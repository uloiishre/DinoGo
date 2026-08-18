package com.dinogo.sysmsg.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.dto.request.auto.*;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.service.OrderMessageService;

/** 僅供可信任 order 模組通知；Request 只接受 orderId。 */
@RestController
@RequestMapping("/api/sysmsg/internal/order-messages")
public class OrderMessageInternalController {
    private final OrderMessageService service;
    private final InternalRequestGuard guard;
    public OrderMessageInternalController(OrderMessageService service,InternalRequestGuard guard){this.service=service;this.guard=guard;}
    @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED)
    public List<SendResponse> event(@RequestHeader("X-Internal-Api-Key") String key,@Valid @RequestBody OrderEventRequest r){guard.verify(key);return service.createOrderEventMessages(r);}
}
