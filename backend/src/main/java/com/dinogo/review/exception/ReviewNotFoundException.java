package com.dinogo.review.exception;

/** 找不到 Review History 或 Star 資源時使用。 */
public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(String message) {
        super(message);
    }
}
