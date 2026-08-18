package com.dinogo.sysmsg.client;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 從目前已驗證的 HTTP Request 轉送 Authorization Bearer Token。 */
@Component
public class CurrentBearerTokenProvider {

    public String getToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("目前沒有可轉送 Bearer Token 的 HTTP Request");
        }

        String authorization = servletAttributes.getRequest().getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("缺少 Authorization: Bearer token");
        }
        return authorization.substring(7);
    }
}
