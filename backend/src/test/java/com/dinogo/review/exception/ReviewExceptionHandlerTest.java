package com.dinogo.review.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

/** 鎖定 Review HTTP 衝突語意。 */
class ReviewExceptionHandlerTest {
    private final ReviewExceptionHandler handler = new ReviewExceptionHandler();

    @Test
    void ownershipFailureReturns403AndMissingReviewReturns404() {
        var forbidden = handler.handleForbidden(new ReviewForbiddenException("not owner"));
        var missing = handler.handleNotFound(new ReviewNotFoundException("missing"));

        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals("REVIEW_FORBIDDEN", forbidden.getBody().get("error"));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertEquals("REVIEW_NOT_FOUND", missing.getBody().get("error"));
    }

    @Test
    void optimisticLockConflictReturnsHttp409() {
        var response = handler.handleConflict(
                new OptimisticLockingFailureException("stale review"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("REVIEW_CONFLICT", response.getBody().get("error"));
    }
}
