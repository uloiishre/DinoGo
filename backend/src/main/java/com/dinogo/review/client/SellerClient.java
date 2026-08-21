package com.dinogo.review.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dinogo.review.dto.external.SellerProductInfoResponse;

@Component
public class SellerClient {

    private final RestClient restClient;

    /*
     * Seller API 目前提供：GET /api/seller/products?sellerId={sellerId}
     * application.yml：
     * seller:
     *   api:
     *     base-url: http://localhost:${SELLER_API_PORT}
     *
     * 商品清單 API 已確認。訂單中的 sellerId 由 Order API 提供，評論模組不再
     * 另外呼叫尚未提供的 GET /api/sellers/{sellerId} 驗證。
     */
    public SellerClient(
            RestClient.Builder restClientBuilder,
            @Value("${seller.api.base-url}") String sellerApiBaseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(sellerApiBaseUrl)
                .build();
    }

    public List<SellerProductInfoResponse> getSellerProducts(Integer sellerId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/seller/products")
                        .queryParam("sellerId", sellerId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<SellerProductInfoResponse>>() {});
    }
}
