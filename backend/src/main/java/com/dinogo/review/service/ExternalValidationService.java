package com.dinogo.review.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.dinogo.review.client.MemberClient;
import com.dinogo.review.client.OrderClient;
import com.dinogo.review.dto.external.MemberInfoResponse;
import com.dinogo.review.dto.external.OrderInfoResponse;
import com.dinogo.review.exception.InvalidOrderStateException;
import com.dinogo.review.exception.ReviewForbiddenException;

@Service
public class ExternalValidationService {

    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private final MemberClient memberClient;
    private final OrderClient orderClient;

    public ExternalValidationService(
            MemberClient memberClient,
            OrderClient orderClient) {

        this.memberClient = memberClient;
        this.orderClient = orderClient;
    }

    /*
     * 取得登入會員資料。
     * Member 模組已負責 JWT 驗證；評論模組只確認回傳資料及 memberId 有效。
     */
    public MemberInfoResponse requireCurrentMember(String authorizationHeader) {
        requireText(authorizationHeader, "缺少 Authorization header");

        MemberInfoResponse member = memberClient.getCurrentMember(authorizationHeader);
        if (member == null || member.memberId() == null) {
            throw new IllegalStateException("會員 API 未回傳有效會員資料");
        }
        return member;
    }

    /*
     * 訂單轉為 COMPLETED 時使用。
     * 訂單 API 是會員、商家及商品關聯資訊的唯一可信來源；評論模組不再呼叫
     * 尚未提供的 GET /api/members/{memberId} 或 GET /api/sellers/{sellerId}。
     * 回傳已驗證的 OrderInfoResponse，供 ReviewService 建立 History 與 Star 快照。
     */
    public OrderInfoResponse requireCompletedOrder(
            Integer orderId,
            String authorizationHeader) {

        OrderInfoResponse order = requireOrder(orderId, authorizationHeader);
        requireStatus(order, COMPLETED);
        return order;
    }

    /*
     * 訂單轉為 CANCELLED 時使用。
     */
    public OrderInfoResponse requireCancelledOrder(
            Integer orderId,
            String authorizationHeader) {

        OrderInfoResponse order = requireOrder(orderId, authorizationHeader);
        requireStatus(order, CANCELLED);
        return order;
    }

    /*
     * 驗證目前登入會員是否擁有該筆評論資料。
     */
    public void requireOwnership(Integer ownerMemberId, Integer currentMemberId) {
        requireId(ownerMemberId, "ownerMemberId");
        requireId(currentMemberId, "currentMemberId");

        if (!ownerMemberId.equals(currentMemberId)) {
            throw new ReviewForbiddenException("目前會員無權操作此評論資料");
        }
    }

    private OrderInfoResponse requireOrder(
            Integer orderId,
            String authorizationHeader) {

        requireId(orderId, "orderId");
        requireText(authorizationHeader, "缺少 Authorization header");

        OrderInfoResponse order = orderClient.getOrder(orderId, authorizationHeader);
        if (order == null || !orderId.equals(order.orderId())) {
            throw new IllegalStateException("訂單 API 回傳資料與 orderId 不一致");
        }
        if (order.buyerId() == null || order.sellerId() == null) {
            throw new IllegalStateException("訂單 API 缺少 buyerId 或 sellerId");
        }
        return order;
    }

    private void requireStatus(OrderInfoResponse order, String expectedStatus) {
        String actualStatus = order.status();
        if (actualStatus == null
                || !expectedStatus.equals(actualStatus.toUpperCase(Locale.ROOT))) {
            throw new InvalidOrderStateException(
                    "訂單狀態必須是 " + expectedStatus + "，目前狀態=" + actualStatus
            );
        }
    }

    private void requireId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " 必須是正整數");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
