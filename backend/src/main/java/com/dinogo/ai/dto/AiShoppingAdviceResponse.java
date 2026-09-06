package com.dinogo.ai.dto;

import java.util.List;

public record AiShoppingAdviceResponse(String summary, AiShoppingCriteria criteria,
        List<AiShoppingRecommendation> recommendations, AiShoppingComparison comparison,
        boolean generatedByAi, String modelName) {
}
