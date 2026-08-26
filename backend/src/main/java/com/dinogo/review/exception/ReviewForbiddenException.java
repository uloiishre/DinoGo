package com.dinogo.review.exception;

/** 目前會員不是評論資源擁有者時使用。 */
public class ReviewForbiddenException extends RuntimeException {

    public ReviewForbiddenException(String message) {
        super(message);
    }
}
