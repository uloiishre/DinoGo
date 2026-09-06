package com.dinogo.ai.dto;

import java.math.BigDecimal;

public record AiShoppingCriteria(String keyword, Integer categoryId, BigDecimal maxPrice, String intentSummary) {
}
