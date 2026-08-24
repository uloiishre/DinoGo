package com.dinogo.sysmsg.entity;

import java.util.Locale;

public enum NotificationType {
    ORDER,
    MARKETING;

    /**
     * 會員收件：AC / AS / SC 是訂單訊息。
     * 商家收件：AC / AS 是訂單訊息。
     * 其他一律是行銷訊息。
     */
    public static NotificationType resolve(String msgFunction, boolean sellerRecipient) {
        if (msgFunction == null || msgFunction.length() < 2) {
            throw new IllegalArgumentException("msgFunction 格式不正確");
        }

        String prefix = msgFunction.substring(0, 2).toUpperCase(Locale.ROOT);
        boolean orderMessage = sellerRecipient
                ? prefix.equals("AC") || prefix.equals("AS")
                : prefix.equals("AC") || prefix.equals("AS") || prefix.equals("SC");

        return orderMessage ? ORDER : MARKETING;
    }
}
