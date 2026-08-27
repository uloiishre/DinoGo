package com.dinogo.sysmsg.service.pagination;

import java.util.List;
import java.util.function.Function;

import com.dinogo.sysmsg.dto.response.CursorPageResponse;

/** Cursor 分頁的容量驗證與回應組裝工具。 */
public final class CursorPageSupport {
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private CursorPageSupport() {
    }

    public static int validateSize(Integer size) {
        int resolved = size == null ? DEFAULT_SIZE : size;
        if (resolved < 1 || resolved > MAX_SIZE) {
            throw new IllegalArgumentException("size 必須介於 1 到 50");
        }
        return resolved;
    }

    public static <S, T> CursorPageResponse<T> response(
            List<S> fetched,
            int size,
            Function<S, T> mapper,
            Function<S, String> cursorFactory) {
        boolean hasNext = fetched.size() > size;
        List<S> visible = hasNext ? fetched.subList(0, size) : fetched;
        List<T> items = visible.stream().map(mapper).toList();
        String nextCursor = hasNext
                ? cursorFactory.apply(visible.get(visible.size() - 1))
                : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }
}
