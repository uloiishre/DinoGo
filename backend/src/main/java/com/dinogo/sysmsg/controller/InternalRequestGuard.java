package com.dinogo.sysmsg.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** order 模組內部端點的最小共享金鑰保護；正式環境由環境變數注入。 */
@Component
public class InternalRequestGuard {
    private final byte[] expected;
    public InternalRequestGuard(@Value("${sysmsg.internal-api-key}") String key) {
        if (key == null || key.isBlank()) throw new IllegalStateException("缺少 sysmsg.internal-api-key");
        this.expected = key.getBytes(StandardCharsets.UTF_8);
    }
    public void verify(String supplied) {
        byte[] actual = supplied == null ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) throw new SecurityException("無效的內部 API 金鑰");
    }
}
