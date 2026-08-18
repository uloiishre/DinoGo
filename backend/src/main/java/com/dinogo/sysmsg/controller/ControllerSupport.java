package com.dinogo.sysmsg.controller;

import org.springframework.stereotype.Component;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.sysmsg.client.MemberClient;

/** Controller 的可信任登入身分來源；不讀取 Request body 內的登入 ID。 */
@Component
public class ControllerSupport {
    private final MemberClient members;
    public ControllerSupport(MemberClient members) { this.members = members; }
    public MemberAuthResponse currentMember() { return members.getProfile(); }
    public Integer memberId() { return currentMember().getMemberId(); }
    public Integer sellerId() {
        Integer id = currentMember().getSellerId();
        if (id == null) throw new SecurityException("目前登入會員不是商家");
        return id;
    }
}
