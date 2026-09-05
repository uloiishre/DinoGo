package com.dinogo.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.review.dto.request.StarUpdateRequest;
import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.entity.StarEntity;
import com.dinogo.review.exception.ReviewConflictException;
import com.dinogo.review.exception.ReviewForbiddenException;
import com.dinogo.review.exception.ReviewNotFoundException;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.review.repository.StarRepository;
import com.dinogo.salesii.dto.OrderReviewItemResponse;
import com.dinogo.salesii.dto.OrderSysmsgResponse;

/** Review 建立、取消、所有權與評論內容異動的正式業務單元測試。 */
class ReviewServiceLifecycleTest {
    private HistoryRepository histories;
    private StarRepository stars;
    private MonolithValidationService validation;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        histories = mock(HistoryRepository.class);
        stars = mock(StarRepository.class);
        validation = mock(MonolithValidationService.class);
        service = new ReviewService(histories, stars, validation, mock(ReviewImageService.class));
        when(histories.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stars.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void completedOrderCreatesOneHistoryAndOneStarPerItem() {
        OrderSysmsgResponse order = order("COMPLETED");
        when(validation.requireCompletedOrder(order)).thenReturn(order);
        when(histories.findByOrderId(10)).thenReturn(Optional.empty());

        var response = service.createHistoryFromCompletedOrder(order);

        assertEquals(10, response.orderId());
        assertEquals("ORD-10", response.orderNo());
        assertEquals(2, response.stars().size());
        verify(histories).saveAndFlush(any(HistoryEntity.class));
    }

    @Test
    void duplicateCompletedOrderReturnsConflict() {
        OrderSysmsgResponse order = order("COMPLETED");
        when(validation.requireCompletedOrder(order)).thenReturn(order);
        when(histories.findByOrderId(10)).thenReturn(Optional.of(new HistoryEntity()));

        assertThrows(ReviewConflictException.class,
                () -> service.createHistoryFromCompletedOrder(order));
    }

    @Test
    void cancelledOrderDeletionIsIdempotent() {
        OrderSysmsgResponse order = order("CANCELLED");
        HistoryEntity history = new HistoryEntity();
        when(validation.requireCancelledOrder(order)).thenReturn(order);
        when(histories.findByOrderId(10)).thenReturn(Optional.of(history)).thenReturn(Optional.empty());

        service.deleteHistoryForCancelledOrder(order);
        service.deleteHistoryForCancelledOrder(order);

        verify(histories, times(1)).delete(history);
    }

    @Test
    void updateDistinguishesForbiddenFromMissingStar() {
        StarUpdateRequest request = request(5, "good");
        when(validation.requireCurrentMember(7)).thenReturn(member(7));
        StarEntity anotherMembersStar = star(8);
        when(stars.findById(1)).thenReturn(Optional.of(anotherMembersStar));
        when(stars.findById(2)).thenReturn(Optional.empty());

        assertThrows(ReviewForbiddenException.class,
                () -> service.updateStar(1, 7, request));
        assertThrows(ReviewNotFoundException.class,
                () -> service.updateStar(2, 7, request));
    }

    @Test
    void updateAndClearKeepSnapshotButReplaceReviewContent() {
        StarEntity star = star(7);
        when(validation.requireCurrentMember(7)).thenReturn(member(7));
        when(stars.findById(1)).thenReturn(Optional.of(star));

        var updated = service.updateStar(1, 7, request(4, "  useful  "));
        assertEquals(4, updated.fiveStar());
        assertEquals("useful", updated.feedback());

        var cleared = service.clearStar(1, 7);
        assertNull(cleared.fiveStar());
        assertNull(cleared.feedback());
        assertEquals(101, cleared.productId());
        assertEquals("Product", cleared.productName());
    }

    private OrderSysmsgResponse order(String status) {
        return new OrderSysmsgResponse(10, "ORD-10", 7, 9, status, List.of(
                new OrderReviewItemResponse(1, 101, "One", null, new BigDecimal("20.00")),
                new OrderReviewItemResponse(2, 102, "Two", "/two.png", new BigDecimal("30.00"))));
    }

    private MemberSysmsgResponse member(int id) {
        return new MemberSysmsgResponse(id, null, true, "member@example.test", "MEMBER",
                List.of(1), true, false);
    }

    private StarEntity star(int ownerId) {
        HistoryEntity history = new HistoryEntity();
        history.setMemberId(ownerId);
        StarEntity star = new StarEntity();
        star.setHistory(history);
        star.setOrderItemId(11);
        star.setProductId(101);
        star.setProductName("Product");
        star.setBasePrice(new BigDecimal("99.00"));
        return star;
    }

    private StarUpdateRequest request(Integer rating, String feedback) {
        return new StarUpdateRequest(rating, feedback, null, null, null, null, null, null);
    }
}
