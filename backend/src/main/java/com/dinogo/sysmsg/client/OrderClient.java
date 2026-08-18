package com.dinogo.sysmsg.client;

import com.dinogo.sysmsg.dto.external.OrderCancelResponse;
import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderClient {

    private final RestClient restClient;
    private final CurrentBearerTokenProvider tokenProvider;

    /*
     * order 模組 API Port
     *
     * 目前模擬環境：http://localhost:8080
     *
     * application.properties：
     *
     * order.api.base-url=http://localhost:8082
     */
    public OrderClient(
            RestClient.Builder restClientBuilder,
            CurrentBearerTokenProvider tokenProvider,
            @Value("${order.api.base-url:http://localhost:8080}") String orderApiBaseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(orderApiBaseUrl)
                .build();
        this.tokenProvider = tokenProvider;
    }

    public OrderInfoResponse getOrder(Integer orderId) {
        return getOrder(orderId, tokenProvider.getToken());
    }


    /*
     * ============================================================
     * 取得訂單資料
     * ============================================================
     *
     * sysmsg 不自行維護：
     *
     * order_id
     * buyer_id
     * seller_id
     * order_no
     * total_amount
     * payment_method_id
     * method_name
     * created_at
     * order_status
     *
     * 上述資料由 order 模組提供。
     *
     * ------------------------------------------------------------
     * 假設 Order API：
     *
     * GET /api/orders/{orderId}
     *
     * ------------------------------------------------------------
     * 回傳：
     *
     * OrderInfoResponse
     *
     * 這個 DTO 是「sysmsg 對 order API 的資料接收物件」。
     * ============================================================
     */
    public OrderInfoResponse getOrder(Integer orderId, String bearerToken) {

        return restClient.get()
                .uri("/api/orders/{orderId}", orderId)
                .headers(headers -> headers.setBearerAuth(normalizeToken(bearerToken)))
                .retrieve()
                .body(OrderInfoResponse.class);
    }

    /** GET /api/orders/member：目前登入會員的歷史訂單。 */
    public List<OrderInfoResponse> getMemberOrders(String bearerToken) {
        OrderInfoResponse[] orders = restClient.get()
                .uri("/api/orders/member")
                .headers(headers -> headers.setBearerAuth(normalizeToken(bearerToken)))
                .retrieve()
                .body(OrderInfoResponse[].class);
        return orders == null ? List.of() : Arrays.asList(orders);
    }

    public List<OrderInfoResponse> getMemberOrders() {
        return getMemberOrders(tokenProvider.getToken());
    }


    /*
     * ============================================================
     * 取得訂單取消資料
     * ============================================================
     *
     * 實際取消資料仍由：
     *
     * GET /api/orders/{orderId}
     *
     * 用於：
     *
     * CANCELLED
     *
     * 取得：
     *
     * order_id
     * total_amount
     * payment_method_id
     * method_name
     * cancel_reason
     * cancelled_at
     *
     * 沒有另外假設 /cancel 查詢端點；sysmsg 不自行維護取消資料。
     * ============================================================
     */
    public OrderInfoResponse getCancelledOrder(Integer orderId, String bearerToken) {
        OrderInfoResponse order = getOrder(orderId, bearerToken);
        if (order == null || !"CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("訂單不是 CANCELLED：" + orderId);
        }
        return order;
    }

    public OrderInfoResponse getCancelledOrder(Integer orderId) {
        return getCancelledOrder(orderId, tokenProvider.getToken());
    }

    /**
     * 假設依賴：若 order 模組後續提供獨立取消明細端點時使用。
     * 目前流程仍可使用 getCancelledOrder() 讀取單筆訂單。
     */
    public OrderCancelResponse getOrderCancel(Integer orderId, String bearerToken) {
        return restClient.get()
                .uri("/api/orders/{orderId}/cancel", orderId)
                .headers(headers -> headers.setBearerAuth(normalizeToken(bearerToken)))
                .retrieve()
                .body(OrderCancelResponse.class);
    }

    public OrderCancelResponse getOrderCancel(Integer orderId) {
        return getOrderCancel(orderId, tokenProvider.getToken());
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("缺少 Bearer Token");
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
