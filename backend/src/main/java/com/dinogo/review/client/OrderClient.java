package com.dinogo.review.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dinogo.review.dto.external.OrderInfoResponse;

@Component
public class OrderClient {

    private final RestClient restClient;

    /*
     * Order API 實際端點：GET /api/orders/{orderId}
     * application.yml：
     * order:
     *   api:
     *     base-url: http://localhost:${ORDER_API_PORT}
     *
     * OrderController 使用 AuthenticatedMember 驗證訂單存取權，
     * 因此評論模組必須轉送原請求的 Authorization header。
     */
    public OrderClient(
            RestClient.Builder restClientBuilder,
            @Value("${order.api.base-url}") String orderApiBaseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(orderApiBaseUrl)
                .build();
    }

    public OrderInfoResponse getOrder(
            Integer orderId,
            String authorizationHeader) {

        return restClient.get()
                .uri("/api/orders/{orderId}", orderId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(OrderInfoResponse.class);
    }
}
