package com.dinogo.ai.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductSemanticSearchService {
    private static final Logger log = LoggerFactory.getLogger(ProductSemanticSearchService.class);
    private static final int MAX_SEMANTIC_RESULTS = 50;
    private static final int MAX_SEMANTIC_CANDIDATES = 8;
    private static final double MINIMUM_SEMANTIC_SCORE = 0.30d;
    private final VectorStoreStateService stateService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public ProductSemanticSearchService(VectorStoreStateService stateService, ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key:}") String configuredApiKey,
            @Value("${OPENAI_API_KEY:}") String environmentApiKey,
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String model,
            @Value("${app.ai.openai.enabled:false}") boolean enabled) {
        this.stateService = stateService; this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(3));
        factory.setReadTimeout(java.time.Duration.ofSeconds(8));
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").requestFactory(factory).build();
        this.apiKey = configuredApiKey == null || configuredApiKey.isBlank() ? environmentApiKey : configuredApiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public List<Integer> searchProductIds(String query) {
        String vectorStoreId = stateService.getVectorStoreId();
        if (!enabled || vectorStoreId == null || vectorStoreId.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Semantic product search skipped: OpenAI is disabled, or its Vector Store ID/API key is unavailable");
            return List.of();
        }
        try {
            JsonNode response = restClient.post().uri("/vector_stores/{id}/search", vectorStoreId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(Map.of("query", query, "max_num_results", MAX_SEMANTIC_RESULTS)).retrieve().body(JsonNode.class);
            java.util.LinkedHashSet<Integer> ids = new java.util.LinkedHashSet<>();
            Pattern productId = Pattern.compile("productId:\\s*(\\d+)");
            for (JsonNode result : response.path("data")) {
                if (result.path("score").asDouble() < MINIMUM_SEMANTIC_SCORE) continue;
                for (JsonNode content : result.path("content")) {
                    var matcher = productId.matcher(content.path("text").asText());
                    while (matcher.find()) ids.add(Integer.valueOf(matcher.group(1)));
                }
                if (ids.size() >= MAX_SEMANTIC_CANDIDATES) break;
            }
            log.info("Semantic product search kept {} candidates above score {}", ids.size(), MINIMUM_SEMANTIC_SCORE);
            return ids.stream().toList();
        } catch (Exception exception) {
            log.warn("Semantic product search failed: {}", exception.getMessage());
            return List.of();
        }
    }
}
