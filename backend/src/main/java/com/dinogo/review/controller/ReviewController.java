package com.dinogo.review.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.review.dto.request.StarUpdateRequest;
import com.dinogo.review.dto.response.HistoryResponse;
import com.dinogo.review.dto.response.ProductReviewPageResponse;
import com.dinogo.review.dto.response.StarResponse;
import com.dinogo.review.service.ReviewService;

import jakarta.validation.Valid;

/**
 * 評論 REST API。
 *
 * <p>Controller 只負責接收 HTTP 資料、執行 @Valid、轉交 Service 及組合回應；
 * 會員身分透過 Authorization 取得，不接受 query parameter 的 memberId。</p>
 */
@Validated
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /*
     * 訂單狀態轉為 COMPLETED 時呼叫。
     * 【假設】Order API 允許帶入此 Authorization 查詢該訂單。
     */
    @PostMapping("/internal/orders/{orderId}/completed")
    public ResponseEntity<HistoryResponse> createHistory(
            @PathVariable Integer orderId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        HistoryResponse response = reviewService.createHistoryFromCompletedOrder(
                orderId,
                authorizationHeader);

        return ResponseEntity
                .created(URI.create("/api/reviews/orders/" + orderId))
                .body(response);
    }

    /*
     * 訂單狀態轉為 CANCELLED 時呼叫。
     * 重複通知採冪等處理。
     */
    @DeleteMapping("/internal/orders/{orderId}/cancelled")
    public ResponseEntity<Void> deleteHistory(
            @PathVariable Integer orderId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        reviewService.deleteHistoryForCancelledOrder(orderId, authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<HistoryResponse> getMemberHistory(
            @PathVariable Integer orderId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        return ResponseEntity.ok(
                reviewService.getMemberHistory(orderId, authorizationHeader));
    }

    // 訂單清單與商品細項由 Order 模組提供；此端點只補上該訂單的評論資料。
    @GetMapping("/orders/{orderId}/stars")
    public ResponseEntity<List<StarResponse>> getMemberStars(
            @PathVariable Integer orderId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        return ResponseEntity.ok(
                reviewService.getMemberStars(orderId, authorizationHeader));
    }

    // 「新增評論」與「修改評論」共用：COMPLETED 時 Star 已預先建立。
    @PutMapping("/stars/{starId}")
    public ResponseEntity<StarResponse> updateStar(
            @PathVariable Integer starId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody StarUpdateRequest request) {

        return ResponseEntity.ok(
                reviewService.updateStar(starId, authorizationHeader, request));
    }

    // 清除評論內容，但不刪除 Star 商品快照。
    @DeleteMapping("/stars/{starId}/content")
    public ResponseEntity<StarResponse> clearStar(
            @PathVariable Integer starId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        return ResponseEntity.ok(
                reviewService.clearStar(starId, authorizationHeader));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductReviewPageResponse> getProductReviews(
            @PathVariable Integer productId,
            @RequestParam(required = false) Integer lastReviewPriority,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime lastStarUpdAt,
            @RequestParam(required = false) Integer lastStarId) {

        return ResponseEntity.ok(
                reviewService.getProductReviews(
                        productId,
                        lastReviewPriority,
                        lastStarUpdAt,
                        lastStarId));
    }
}
