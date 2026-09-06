package com.dinogo.ai.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.dinogo.ai.dto.SemanticIndexRebuildResponse;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.service.ProductService;
import tools.jackson.databind.JsonNode;

@Service
public class ProductSemanticIndexService {
    private static final Duration INDEX_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private final ProductService productService;
    private final RestClient restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    private final String apiKey;
    private final VectorStoreStateService vectorStoreStateService;
    public ProductSemanticIndexService(ProductService productService, @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${OPENAI_API_KEY:}") String environmentApiKey, VectorStoreStateService vectorStoreStateService) {
        this.productService = productService;
        this.apiKey = apiKey == null || apiKey.isBlank() ? environmentApiKey : apiKey;
        this.vectorStoreStateService = vectorStoreStateService;
    }
    public SemanticIndexRebuildResponse rebuild() {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OpenAI API key is not configured");
        var products = productService.getProducts(null, null, null, null, null, null, null, null, 0, 100, "salesDesc").getContent();
        JsonNode store = restClient.post().uri("/vector_stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("name", "DinoGo Product Index")).retrieve().body(JsonNode.class);
        String storeId = store.path("id").asText();
        List<CompletableFuture<String>> attachmentFutures = products.stream()
                .map(product -> CompletableFuture.supplyAsync(() -> uploadAndAttach(storeId, product)))
                .toList();

        List<String> attachmentIds;
        try {
            attachmentIds = attachmentFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (CompletionException exception) {
            return new SemanticIndexRebuildResponse(storeId, products.size(), "failed");
        }

        String status = waitForAllCompletion(storeId, attachmentIds);
        if ("completed".equals(status)) vectorStoreStateService.save(storeId);
        return new SemanticIndexRebuildResponse(storeId, products.size(), status);
    }

    private String uploadAndAttach(String storeId, ProductResponse product) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("purpose", "assistants");
        form.add("file", new ByteArrayResource(document(product).getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return "product-" + product.getProductId() + ".txt"; }
        });

        JsonNode file = restClient.post().uri("/files")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(JsonNode.class);
        JsonNode attachment = restClient.post().uri("/vector_stores/{id}/files", storeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("file_id", file.path("id").asText())).retrieve().body(JsonNode.class);
        return attachment.path("id").asText();
    }

    private String document(ProductResponse p) { return "productId: " + p.getProductId() + "\nname: " + p.getProductName() + "\ndescription: " + (p.getDescription() == null ? "" : p.getDescription()) + "\nprice: " + p.getMinPrice(); }

    private String waitForAllCompletion(String storeId, List<String> attachmentIds) {
        Set<String> pendingIds = new LinkedHashSet<>(attachmentIds);
        Instant deadline = Instant.now().plus(INDEX_TIMEOUT);

        while (!pendingIds.isEmpty() && Instant.now().isBefore(deadline)) {
            for (String attachmentId : new ArrayList<>(pendingIds)) {
                JsonNode current = restClient.get().uri("/vector_stores/{storeId}/files/{fileId}", storeId, attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).retrieve().body(JsonNode.class);
                String status = current.path("status").asText("in_progress");
                if ("completed".equals(status)) {
                    pendingIds.remove(attachmentId);
                } else if ("failed".equals(status) || "cancelled".equals(status)) {
                    return status;
                }
            }

            if (!pendingIds.isEmpty()) {
                long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
                if (!sleep(Math.min(POLL_INTERVAL.toMillis(), remainingMillis))) return "interrupted";
            }
        }
        return pendingIds.isEmpty() ? "completed" : "timeout";
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
