package com.dinogo.ai.dto;

import java.util.List;
import com.dinogo.catalog.dto.ProductResponse;

public record AiShoppingRecommendation(ProductResponse product, List<String> reasons) {
}
