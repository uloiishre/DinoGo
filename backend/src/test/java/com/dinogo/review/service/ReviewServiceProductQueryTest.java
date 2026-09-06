package com.dinogo.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.dinogo.review.dto.response.ProductReviewPageResponse;
import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.entity.StarEntity;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.review.repository.ProductReviewAggregate;
import com.dinogo.review.repository.SellerRatingAggregate;
import com.dinogo.review.repository.StarRepository;

/** Review 商品列表採 Offset pagination；API 頁碼一基底、Repository 頁碼零基底。 */
class ReviewServiceProductQueryTest {

    private StarRepository stars;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        stars = mock(StarRepository.class);
        ProductReviewAggregate aggregate = mock(ProductReviewAggregate.class);
        when(stars.aggregateProductReviews(9)).thenReturn(aggregate);
        service = new ReviewService(
                mock(HistoryRepository.class),
                stars,
                mock(MonolithValidationService.class),
                mock(ReviewImageService.class));
    }

    @Test
    void offsetPaginationForwardsFiltersAndUsesTenItemsPerPage() {
        when(stars.findPublicProductReviews(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(3), 0));

        ProductReviewPageResponse response = service.getProductReviews(9, 1, 5, "feedback");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(stars).findPublicProductReviews(
                org.mockito.ArgumentMatchers.eq(9),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("FEEDBACK"),
                pageable.capture());
        assertEquals(10, pageable.getValue().getPageSize());
        assertIterableEquals(
                List.of("reviewPriority", "starUpdAt", "id"),
                pageable.getValue().getSort().stream().map(order -> order.getProperty()).toList());
        assertEquals(1, response.currentPage());
    }

    @Test
    void rejectsUnsupportedContentFilter() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getProductReviews(9, 1, null, "video"));
    }

    @Test
    void offsetPaginationConvertsSecondApiPageAndReturnsPageMetadata() {
        StarEntity row = mock(StarEntity.class);
        HistoryEntity history = mock(HistoryEntity.class);
        when(row.getHistory()).thenReturn(history);
        when(history.getMemberId()).thenReturn(7);
        when(stars.findPublicProductReviews(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(row), invocation.getArgument(3), 25));

        ProductReviewPageResponse response = service.getProductReviews(9, 2, null, "ALL");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(stars).findPublicProductReviews(
                org.mockito.ArgumentMatchers.eq(9),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("ALL"),
                pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getOffset());
        assertEquals(2, response.currentPage());
        assertEquals(3, response.totalPages());
        assertEquals(25, response.totalElements());
    }

    @Test
    void offsetPaginationRejectsPageBeyondLastResultBoundary() {
        when(stars.findPublicProductReviews(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(3), 11));

        assertThrows(IllegalArgumentException.class,
                () -> service.getProductReviews(9, 3, null, "ALL"));
    }

    @Test
    void productAndSellerAveragesAreTruncatedNotRounded() {
        ProductReviewAggregate product = mock(ProductReviewAggregate.class);
        when(product.getAverageFiveStar()).thenReturn(new BigDecimal("4.99"));
        when(stars.aggregateProductReviews(10)).thenReturn(product);
        SellerRatingAggregate seller = mock(SellerRatingAggregate.class);
        when(seller.getAverageFiveStar()).thenReturn(new BigDecimal("3.19"));
        when(stars.aggregateSellerRatings(20)).thenReturn(seller);

        assertEquals(new BigDecimal("4.9"), service.getProductRatingSummary(10).averageFiveStar());
        assertEquals(new BigDecimal("3.1"), service.getSellerRatingSummary(20).averageFiveStar());
    }
}
