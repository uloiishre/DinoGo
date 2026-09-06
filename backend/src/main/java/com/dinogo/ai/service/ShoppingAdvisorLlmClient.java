package com.dinogo.ai.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dinogo.ai.dto.AiShoppingCriteria;
import com.dinogo.catalog.dto.CategoryResponse;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShoppingAdvisorLlmClient {
    private static final Logger log = LoggerFactory.getLogger(ShoppingAdvisorLlmClient.class);
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public ShoppingAdvisorLlmClient(ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key:}") String configuredApiKey,
            @Value("${OPENAI_API_KEY:}") String environmentApiKey,
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String model,
            @Value("${app.ai.openai.enabled:false}") boolean enabled) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").requestFactory(factory).build();
        this.apiKey = configuredApiKey == null || configuredApiKey.isBlank() ? environmentApiKey : configuredApiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public Result parse(String message) {
        return parse(message, List.of());
    }

    public Result parse(String message, List<CategoryResponse> categories) {
        if (!enabled || apiKey == null || apiKey.isBlank()) return new Result(null, false, null);
        try {
            Map<String, Object> schema = Map.of("type", "object", "additionalProperties", false,
                    "required", List.of("keyword", "categoryId", "maxPrice", "intentSummary"), "properties", Map.of(
                            "keyword", Map.of("type", "string"),
                            "categoryId", Map.of("type", List.of("integer", "null")),
                            "maxPrice", Map.of("type", List.of("number", "null")),
                            "intentSummary", Map.of("type", "string")));
            List<Map<String, Object>> availableCategories = categories.stream()
                    .map(category -> Map.<String, Object>of("categoryId", category.getCategoryId(),
                            "categoryName", category.getCategoryName()))
                    .toList();
            JsonNode response = restClient.post().uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(Map.of("model", model, "store", false, "max_output_tokens", 180,
                            "instructions", "Extract shopping criteria from userRequest. Return only data matching the supplied JSON schema. keyword must describe the requested product or use case. maxPrice is the user's maximum budget or null. categoryId must be one of availableCategories or null. intentSummary must briefly describe the request.",
                            "input", objectMapper.writeValueAsString(Map.of("userRequest", message, "availableCategories", availableCategories)),
                            "text", Map.of("format", Map.of("type", "json_schema", "name", "shopping_criteria", "strict", true, "schema", schema))))
                    .retrieve().body(JsonNode.class);
            String text = outputText(response);
            if (text.isBlank()) throw new IllegalArgumentException("Empty LLM response");
            AiShoppingCriteria criteria = objectMapper.readValue(text, AiShoppingCriteria.class);
            if (criteria.keyword() == null || criteria.keyword().isBlank()
                    || (criteria.maxPrice() != null && criteria.maxPrice().signum() < 0)) {
                throw new IllegalArgumentException("Invalid LLM criteria");
            }
            return new Result(criteria, true, model);
        } catch (Exception exception) {
            log.warn("AI shopping criteria parsing failed: {}", exception.getMessage());
            return new Result(null, false, null);
        }
    }

    public record Result(AiShoppingCriteria criteria, boolean generatedByAi, String modelName) {}

    private String outputText(JsonNode response) {
        String outputText = response.path("output_text").asText();
        if (!outputText.isBlank()) return outputText;
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                String text = content.path("text").asText();
                if (!text.isBlank()) return text;
            }
        }
        return "";
    }
}
