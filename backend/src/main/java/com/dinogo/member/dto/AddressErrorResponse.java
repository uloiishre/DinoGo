package com.dinogo.member.dto;

/** Address API 的錯誤格式，讓前端統一讀取 message。 */
public record AddressErrorResponse(String message) {
}
