package com.dinogo.sysmsg.service.pagination;

import java.util.Locale;

/** Sysmsg cursor 分頁支援的時間排序方向。 */
public enum CursorSort {
    NEWEST,
    OLDEST;

    public static CursorSort parse(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("sort 只允許 NEWEST 或 OLDEST");
        }
    }
}
