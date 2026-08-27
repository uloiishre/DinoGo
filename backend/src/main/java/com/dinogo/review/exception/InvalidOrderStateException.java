package com.dinogo.review.exception;

/** 訂單狀態不符合建立或刪除評論條件時使用。 */
public class InvalidOrderStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
