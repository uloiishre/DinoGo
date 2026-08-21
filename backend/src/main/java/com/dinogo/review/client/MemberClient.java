package com.dinogo.review.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dinogo.review.dto.external.MemberInfoResponse;

@Component
public class MemberClient {

    private final RestClient restClient;

    /*
     * Member API 實際端點：GET /api/member/profile
     * application.yml：
     * member:
     *   api:
     *     base-url: http://localhost:${MEMBER_API_PORT}
     *
     * 此端點透過 JWT 的 AuthenticatedMember 取得目前會員，
     * 所以評論模組必須轉送原請求的 Authorization header。
     */
    public MemberClient(
            RestClient.Builder restClientBuilder,
            @Value("${member.api.base-url}") String memberApiBaseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(memberApiBaseUrl)
                .build();
    }

    public MemberInfoResponse getCurrentMember(String authorizationHeader) {
        return restClient.get()
                .uri("/api/member/profile")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(MemberInfoResponse.class);
    }

}
