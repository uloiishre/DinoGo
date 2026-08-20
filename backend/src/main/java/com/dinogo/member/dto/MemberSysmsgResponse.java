package com.dinogo.member.dto;

/**
 * Member data supplied to the system-message and review modules.
 */
public record MemberSysmsgResponse(
        Integer memberId,
        Integer sellerId,
        boolean authenticated,
        String email,
        String role,
        Boolean emailOrderNotifications,
        Boolean emailMarketingNotifications) {
}
