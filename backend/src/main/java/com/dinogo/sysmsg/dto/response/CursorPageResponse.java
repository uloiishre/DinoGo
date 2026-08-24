package com.dinogo.sysmsg.dto.response;

import java.util.List;

/** 無限滾動查詢的統一回應；nextCursor 為 null 表示沒有下一頁。 */
public record CursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext) {
    public CursorPageResponse {
        items = items == null ? List.of() : List.copyOf(items);
        if (!hasNext) {
            nextCursor = null;
        }
    }
}
