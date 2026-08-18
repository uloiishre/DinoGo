package com.dinogo.sysmsg.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.dto.request.send.SellerCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.service.SendService;

@RestController
@RequestMapping("/api/sysmsg/seller/messages")
public class SellerMessageController {
    private final SendService service; private final ControllerSupport auth;
    public SellerMessageController(SendService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SendResponse create(@Valid @RequestBody SellerCreateRequest request){return service.createSellerSend(request,auth.memberId());}
    @GetMapping("/outbox") public List<SendResponse> outbox(){return service.findSellerOutbox(auth.memberId());}
}
