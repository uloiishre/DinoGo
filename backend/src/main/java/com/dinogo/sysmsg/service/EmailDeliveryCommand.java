package com.dinogo.sysmsg.service;

/** 送交單封 Email 給郵件 provider 時需要的不可變資料。 */
public record EmailDeliveryCommand(
        Integer recordChannelId,
        String recipientEmail,
        String subject,
        String content) {
}
