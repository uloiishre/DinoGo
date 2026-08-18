package com.dinogo.sysmsg.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.sysmsg.dto.external.SellerInfoResponse;

/**
 * 目前沒有另外提供 seller 查詢端點，因此從已驗證的會員 profile 取得 sellerId。
 * 不接受前端自行宣告 sellerId。
 */
@Component
public class SellerClient {

    private final MemberClient memberClient;
    private final RestClient restClient;
    private final CurrentBearerTokenProvider tokenProvider;

    public SellerClient(
            MemberClient memberClient,
            CurrentBearerTokenProvider tokenProvider,
            RestClient.Builder builder,
            @Value("${seller.api.base-url:http://localhost:8080}") String baseUrl) {
        this.memberClient = memberClient;
        this.tokenProvider = tokenProvider;
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public SellerInfoResponse getCurrentSeller(String bearerToken) {
        MemberAuthResponse member = memberClient.getProfile(bearerToken);
        if (member.getSellerId() == null) {
            throw new IllegalStateException("目前登入會員不是商家");
        }

        SellerInfoResponse seller = new SellerInfoResponse();
        seller.setSellerId(member.getSellerId());
        seller.setMemberId(member.getMemberId());
        seller.setActive(true);
        return seller;
    }

    /**
     * 假設依賴：其他模組完成後，用於驗證任意收件商家或訂單商家。
     * 預定端點 GET /api/sellers/{sellerId}。
     */
    public SellerInfoResponse getSeller(Integer sellerId, String bearerToken) {
        SellerInfoResponse seller = restClient.get()
                .uri("/api/sellers/{sellerId}", sellerId)
                .headers(headers -> headers.setBearerAuth(normalizeToken(bearerToken)))
                .retrieve()
                .body(SellerInfoResponse.class);
        if (seller == null || seller.getSellerId() == null) {
            throw new IllegalStateException("商家資料回傳不完整：" + sellerId);
        }
        return seller;
    }

    public SellerInfoResponse getSeller(Integer sellerId) {
        return getSeller(sellerId, tokenProvider.getToken());
    }

    /** 假設端點 GET /api/sellers，供 OA 系統公告取得所有商家收件人。 */
    public List<SellerInfoResponse> getAllSellers() {
        SellerInfoResponse[] result = restClient.get()
                .uri("/api/sellers")
                .headers(headers -> headers.setBearerAuth(normalizeToken(tokenProvider.getToken())))
                .retrieve().body(SellerInfoResponse[].class);
        return result == null ? List.of() : Arrays.stream(result)
                .filter(seller -> seller != null && seller.getSellerId() != null && seller.isActive()).toList();
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("缺少 Bearer Token");
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
