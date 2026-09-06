package com.dinogo.ai.dto;

public record SemanticIndexRebuildResponse(String vectorStoreId, int indexedProductCount, String status) {
}
