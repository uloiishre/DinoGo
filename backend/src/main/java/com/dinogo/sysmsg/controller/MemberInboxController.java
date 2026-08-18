package com.dinogo.sysmsg.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.entity.RecordStatus;
import com.dinogo.sysmsg.dto.request.record.RecordStatusUpdateRequest;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.service.RecordService;

@RestController
@RequestMapping("/api/sysmsg/member/inbox")
public class MemberInboxController {
    private final RecordService service; private final ControllerSupport auth;
    public MemberInboxController(RecordService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    @GetMapping public List<MsgInboxResponse> inbox(@RequestParam String category){return service.getMemberInbox(auth.memberId(),category);}
    @GetMapping("/{recordId}") public RecordResponse get(@PathVariable Integer recordId){return service.searchRecord(recordId,auth.memberId());}
    @PatchMapping("/{recordId}/status") public RecordResponse status(@PathVariable Integer recordId,@Valid @RequestBody RecordStatusUpdateRequest request){
        if(request.getTargetStatus()!=RecordStatus.READ)throw new IllegalArgumentException("收件匣狀態端點只允許 UNREAD → READ");
        return service.readRecord(recordId,auth.memberId());
    }
    @DeleteMapping("/{recordId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer recordId){service.deleteRecord(recordId,auth.memberId());}
}
