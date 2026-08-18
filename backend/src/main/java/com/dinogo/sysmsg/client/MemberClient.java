package com.dinogo.sysmsg.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dinogo.sysmsg.dto.external.MemberAuthResponse;

/** 會員登入模組 Client；登入者身分一律由 Bearer Token 對應的 profile 取得。 */
@Component
public class MemberClient {

    private final RestClient restClient;
    private final CurrentBearerTokenProvider tokenProvider;

    public MemberClient(
            RestClient.Builder builder,
            CurrentBearerTokenProvider tokenProvider,
            @Value("${member.api.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.tokenProvider = tokenProvider;
    }

    public MemberAuthResponse getProfile() {
        return getProfile(tokenProvider.getToken());
    }

    /** GET /api/member/profile */
    public MemberAuthResponse getProfile(String bearerToken) {
        MemberAuthResponse profile = restClient.get()
                .uri("/api/member/profile")
                .headers(headers -> headers.setBearerAuth(normalizeToken(bearerToken)))
                .retrieve()
                .body(MemberAuthResponse.class);

        if (profile == null || profile.getMemberId() == null) {
            throw new IllegalStateException("會員 profile 回傳資料不完整");
        }
        profile.setAuthenticated(true);
        return profile;
    }

    /**
     * 假設依賴：其他模組完成後，用於系統發訊前驗證任意收件會員存在。
     * 預定端點 GET /api/members/{memberId}；保留但不取代目前的 profile 驗證。
     */
    public MemberAuthResponse getMember(Integer memberId, String bearerToken) {
        MemberAuthResponse member = restClient.get()
                .uri("/api/members/{memberId}", memberId)
                .headers(headers -> headers.setBearerAuth(normalizeToken(bearerToken)))
                .retrieve()
                .body(MemberAuthResponse.class);
        if (member == null || member.getMemberId() == null) {
            throw new IllegalStateException("會員資料回傳不完整：" + memberId);
        }
        return member;
    }

    public MemberAuthResponse getMember(Integer memberId) {
        return getMember(memberId, tokenProvider.getToken());
    }

    /** 假設端點 GET /api/members，供 OA 系統公告取得所有會員收件人。 */
    public List<MemberAuthResponse> getAllMembers() {
        MemberAuthResponse[] result = restClient.get()
                .uri("/api/members")
                .headers(headers -> headers.setBearerAuth(normalizeToken(tokenProvider.getToken())))
                .retrieve().body(MemberAuthResponse[].class);
        return result == null ? List.of() : Arrays.stream(result)
                .filter(member -> member != null && member.getMemberId() != null).toList();
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("缺少 Bearer Token");
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
