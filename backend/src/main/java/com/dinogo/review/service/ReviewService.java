package com.dinogo.review.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.entity.StarEntity;
import com.dinogo.review.dto.external.MemberInfoResponse;
import com.dinogo.review.dto.external.OrderInfoResponse;
import com.dinogo.review.dto.external.OrderItemInfoResponse;
import com.dinogo.review.dto.request.StarUpdateRequest;
import com.dinogo.review.dto.response.HistoryResponse;
import com.dinogo.review.dto.response.ProductReviewCursor;
import com.dinogo.review.dto.response.ProductReviewPageResponse;
import com.dinogo.review.dto.response.ProductReviewResponse;
import com.dinogo.review.dto.response.StarResponse;
import com.dinogo.review.exception.ReviewConflictException;
import com.dinogo.review.exception.ReviewNotFoundException;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.review.repository.StarRepository;

/**
 * 評論模組主要業務層。
 *
 * <p>負責 COMPLETED 建立、CANCELLED 刪除、會員所有權、評論修改／清除與產品頁分頁。
 * 所有寫入流程均在 transaction 內完成；Repository 只負責資料存取。</p>
 */
@Service
public class ReviewService {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final HistoryRepository historyRepository;
    private final StarRepository starRepository;
    private final ExternalValidationService externalValidationService;

    public ReviewService(
            HistoryRepository historyRepository,
            StarRepository starRepository,
            ExternalValidationService externalValidationService) {

        this.historyRepository = historyRepository;
        this.starRepository = starRepository;
        this.externalValidationService = externalValidationService;
    }

    // COMPLETED：訂單資料是唯一可信來源，前端不得自行提供商品快照。
    @Transactional
    public HistoryResponse createHistoryFromCompletedOrder(
            Integer orderId,
            String authorizationHeader) {

        OrderInfoResponse order = externalValidationService.requireCompletedOrder(
                orderId,
                authorizationHeader);

        if (historyRepository.findByOrderId(orderId).isPresent()) {
            throw new ReviewConflictException("此訂單已建立評論紀錄，orderId=" + orderId);
        }
        if (order.items() == null || order.items().isEmpty()) {
            throw new IllegalStateException("訂單 API 未回傳商品明細");
        }

        HistoryEntity history = new HistoryEntity();
        history.setMemberId(order.buyerId());
        history.setSellerId(order.sellerId());
        history.setOrderId(order.orderId());

        for (OrderItemInfoResponse item : order.items()) {
            history.addStar(toStarSnapshot(item));
        }

        try {
            // 立即 flush，讓 UQ_review_history_order 的併發衝突在此處被捕捉。
            HistoryEntity savedHistory = historyRepository.saveAndFlush(history);
            return HistoryResponse.fromEntity(savedHistory, savedHistory.getStars());
        } catch (DataIntegrityViolationException exception) {
            throw new ReviewConflictException(
                    "訂單評論已由另一個請求建立，orderId=" + orderId,
                    exception);
        }
    }

    // CANCELLED：重複通知採冪等處理；History 不存在也視為成功。
    @Transactional
    public void deleteHistoryForCancelledOrder(
            Integer orderId,
            String authorizationHeader) {

        externalValidationService.requireCancelledOrder(orderId, authorizationHeader);
        historyRepository.findByOrderId(orderId).ifPresent(historyRepository::delete);
    }

    @Transactional(readOnly = true)
    public HistoryResponse getMemberHistory(
            Integer orderId,
            String authorizationHeader) {

        MemberInfoResponse member = externalValidationService.requireCurrentMember(authorizationHeader);
        HistoryEntity history = requireHistory(orderId);
        externalValidationService.requireOwnership(history.getMemberId(), member.memberId());

        List<StarEntity> stars = starRepository.findByHistoryIdOrderByOrderItemIdAsc(history.getId());
        return HistoryResponse.fromEntity(history, stars);
    }

    @Transactional(readOnly = true)
    public List<StarResponse> getMemberStars(
            Integer orderId,
            String authorizationHeader) {

        MemberInfoResponse member = externalValidationService.requireCurrentMember(authorizationHeader);
        HistoryEntity history = requireHistory(orderId);
        externalValidationService.requireOwnership(history.getMemberId(), member.memberId());

        return starRepository.findByHistoryIdOrderByOrderItemIdAsc(history.getId())
                .stream()
                .map(StarResponse::fromEntity)
                .toList();
    }

    @Transactional
    public StarResponse updateStar(
            Integer starId,
            String authorizationHeader,
            StarUpdateRequest request) {

        MemberInfoResponse member = externalValidationService.requireCurrentMember(authorizationHeader);
        validateImages(request.imgOne(), request.imgTwo(), request.imgThree());

        StarEntity star = starRepository.findByIdAndHistoryMemberId(starId, member.memberId())
                .orElseThrow(() -> new ReviewNotFoundException(
                        "找不到評論，或目前會員無權操作，starId=" + starId));

        star.setFiveStar(request.fiveStar());
        star.setFeedback(normalizeFeedback(request.feedback()));
        star.setImgOne(request.imgOne());
        star.setImgTwo(request.imgTwo());
        star.setImgThree(request.imgThree());
        star.setStarUpdAt(LocalDateTime.now());

        StarEntity savedStar = starRepository.saveAndFlush(star);
        return StarResponse.fromEntity(savedStar);
    }

    @Transactional
    public StarResponse clearStar(Integer starId, String authorizationHeader) {
        MemberInfoResponse member = externalValidationService.requireCurrentMember(authorizationHeader);

        StarEntity star = starRepository.findByIdAndHistoryMemberId(starId, member.memberId())
                .orElseThrow(() -> new ReviewNotFoundException(
                        "找不到評論，或目前會員無權操作，starId=" + starId));

        star.setFiveStar(null);
        star.setFeedback(null);
        star.setImgOne(null);
        star.setImgTwo(null);
        star.setImgThree(null);
        star.setStarUpdAt(LocalDateTime.now());

        StarEntity savedStar = starRepository.saveAndFlush(star);
        return StarResponse.fromEntity(savedStar);
    }

    @Transactional(readOnly = true)
    public ProductReviewPageResponse getProductReviews(
            Integer productId,
            Integer lastReviewPriority,
            LocalDateTime lastStarUpdAt,
            Integer lastStarId) {

        requirePositiveId(productId, "productId");
        validateCursor(lastReviewPriority, lastStarUpdAt, lastStarId);

        // 第一頁與下一頁使用不同 SQL，避免 nullable cursor 的 OR 條件影響索引計畫。
        List<StarEntity> reviews = lastStarId == null
                ? starRepository.findFirstProductReviewPage(productId)
                : starRepository.findNextProductReviewPage(
                        productId,
                        lastReviewPriority,
                        lastStarUpdAt,
                        lastStarId);

        boolean hasNext = reviews.size() > PAGE_SIZE;
        List<StarEntity> page = hasNext ? reviews.subList(0, PAGE_SIZE) : reviews;
        List<ProductReviewResponse> content = page.stream()
                .map(ProductReviewResponse::fromEntity)
                .toList();

        ProductReviewCursor nextCursor = null;
        if (hasNext) {
            StarEntity last = page.get(page.size() - 1);
            nextCursor = new ProductReviewCursor(
                    last.getReviewPriority(),
                    last.getStarUpdAt(),
                    last.getId());
        }
        return new ProductReviewPageResponse(content, hasNext, nextCursor);
    }

    private HistoryEntity requireHistory(Integer orderId) {
        requirePositiveId(orderId, "orderId");
        return historyRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "找不到此訂單的評論紀錄，orderId=" + orderId));
    }

    private StarEntity toStarSnapshot(OrderItemInfoResponse item) {
        if (item == null
                || item.orderItemId() == null
                || item.productId() == null
                || item.productName() == null
                || item.productImageUrl() == null
                || item.unitPrice() == null) {
            throw new IllegalStateException("訂單商品明細缺少評論快照必要欄位");
        }
        if (item.productName().length() > 50) {
            throw new IllegalStateException("訂單商品名稱超過 review.star.product_name 長度 50");
        }
        if (item.productImageUrl().length() > 255) {
            throw new IllegalStateException("訂單商品圖片 URL 超過長度 255");
        }

        StarEntity star = new StarEntity();
        star.setOrderItemId(item.orderItemId());
        star.setProductId(item.productId());
        star.setProductName(item.productName());
        star.setImageUrl(item.productImageUrl());
        star.setBasePrice(item.unitPrice());
        return star;
    }

    private void validateCursor(
            Integer priority,
            LocalDateTime updatedAt,
            Integer starId) {

        int supplied = (priority == null ? 0 : 1)
                + (updatedAt == null ? 0 : 1)
                + (starId == null ? 0 : 1);
        if (supplied != 0 && supplied != 3) {
            throw new IllegalArgumentException("分頁 cursor 必須三個全部提供或全部省略");
        }
        if (supplied == 3) {
            if (priority < 0 || priority > 2) {
                throw new IllegalArgumentException("lastReviewPriority 必須介於 0～2");
            }
            requirePositiveId(starId, "lastStarId");
        }
    }

    private void validateImages(byte[]... images) {
        for (byte[] image : images) {
            if (image == null) {
                continue;
            }
            if (image.length == 0) {
                throw new IllegalArgumentException("評論圖片不可為空檔案");
            }
            if (image.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("每張評論圖片不可超過 5 MB");
            }
            if (!hasSupportedImageSignature(image)) {
                throw new IllegalArgumentException("評論圖片只接受 PNG、JPEG、GIF 或 WebP");
            }
        }
    }

    // 驗證實際檔頭，不信任前端提供的副檔名或 MIME type。
    private boolean hasSupportedImageSignature(byte[] image) {
        boolean png = image.length >= 8
                && unsigned(image[0]) == 0x89
                && image[1] == 'P' && image[2] == 'N' && image[3] == 'G'
                && unsigned(image[4]) == 0x0D && unsigned(image[5]) == 0x0A
                && unsigned(image[6]) == 0x1A && unsigned(image[7]) == 0x0A;
        boolean jpeg = image.length >= 3
                && unsigned(image[0]) == 0xFF
                && unsigned(image[1]) == 0xD8
                && unsigned(image[2]) == 0xFF;
        boolean gif = image.length >= 6
                && image[0] == 'G' && image[1] == 'I' && image[2] == 'F'
                && image[3] == '8'
                && (image[4] == '7' || image[4] == '9')
                && image[5] == 'a';
        boolean webp = image.length >= 12
                && image[0] == 'R' && image[1] == 'I' && image[2] == 'F' && image[3] == 'F'
                && image[8] == 'W' && image[9] == 'E' && image[10] == 'B' && image[11] == 'P';
        return png || jpeg || gif || webp;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String normalizeFeedback(String feedback) {
        if (feedback == null) {
            return null;
        }
        String normalized = feedback.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void requirePositiveId(Integer id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(name + " 必須是正整數");
        }
    }
}
