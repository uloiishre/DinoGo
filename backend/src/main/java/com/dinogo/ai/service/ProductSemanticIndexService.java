package com.dinogo.ai.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.dinogo.ai.dto.SemanticIndexRebuildResponse;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.service.ProductService;

import tools.jackson.databind.JsonNode;

@Service
public class ProductSemanticIndexService {
    private static final Logger log = LoggerFactory.getLogger(ProductSemanticIndexService.class);
    private static final Duration INDEX_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration MAX_OPENAI_REQUEST_DURATION = Duration.ofSeconds(11);
    private static final int PRODUCT_PAGE_SIZE = 100;

    private final ProductService productService;
    private final RestClient restClient;
    private final String apiKey;
    private final VectorStoreStateService vectorStoreStateService;

    public ProductSemanticIndexService(ProductService productService, @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${OPENAI_API_KEY:}") String environmentApiKey, VectorStoreStateService vectorStoreStateService) {
        this(productService, apiKey, environmentApiKey, vectorStoreStateService, createRestClient());
    }

    ProductSemanticIndexService(ProductService productService, String apiKey, String environmentApiKey,
            VectorStoreStateService vectorStoreStateService, RestClient restClient) {
        this.productService = productService;
        this.restClient = restClient;
        this.apiKey = apiKey == null || apiKey.isBlank() ? environmentApiKey : apiKey;
        this.vectorStoreStateService = vectorStoreStateService;
    }

    private static RestClient createRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().baseUrl("https://api.openai.com/v1").requestFactory(factory).build();
    }

    public synchronized SemanticIndexRebuildResponse rebuild() {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OpenAI API key is not configured");

        List<ProductResponse> products = loadAllActiveProducts();
        VectorStoreStateService.VectorStoreState previousState = retryPendingCleanup(vectorStoreStateService.getState());
        Set<String> newFileIds = ConcurrentHashMap.newKeySet();
        String newStoreId = null;

        try {
            newStoreId = createStore();
            String storeId = newStoreId;
            List<CompletableFuture<IndexedFile>> attachmentFutures = products.stream()
                    .map(product -> CompletableFuture.supplyAsync(() -> uploadAndAttach(storeId, product, newFileIds)))
                    .toList();
            List<IndexedFile> indexedFiles = awaitAttachments(attachmentFutures);
            if (indexedFiles == null) return failAndCleanup(previousState, newStoreId, newFileIds, products.size(), "failed");

            String status = waitForAllCompletion(newStoreId, indexedFiles.stream().map(IndexedFile::attachmentId).toList());
            if (!"completed".equals(status)) return failAndCleanup(previousState, newStoreId, newFileIds, products.size(), status);

            // Persist all old resources as pending before switching the active Store ID.
            List<VectorStoreStateService.CleanupTarget> pendingCleanup = new ArrayList<>(previousState.pendingCleanup());
            if (previousState.vectorStoreId() != null && !previousState.vectorStoreId().isBlank()
                    && !previousState.vectorStoreId().equals(newStoreId)) {
                pendingCleanup.add(enrichCleanupTarget(new VectorStoreStateService.CleanupTarget(
                        previousState.vectorStoreId(), previousState.fileIds())));
            }
            VectorStoreStateService.VectorStoreState newState = new VectorStoreStateService.VectorStoreState(
                    newStoreId, List.copyOf(newFileIds), pendingCleanup);
            vectorStoreStateService.save(newState);
            try {
                retryPendingCleanup(newState);
            } catch (Exception exception) {
                // The new active Store and pending cleanup list were already persisted above.
                log.warn("Pending OpenAI resource cleanup will be retried on the next rebuild: {}", exception.getMessage());
            }
            return new SemanticIndexRebuildResponse(newStoreId, products.size(), "completed");
        } catch (Exception exception) {
            log.warn("Semantic index rebuild failed: {}", exception.getMessage());
            return failAndCleanup(previousState, newStoreId, newFileIds, products.size(), "failed");
        }
    }

    private String createStore() {
        JsonNode store = restClient.post().uri("/vector_stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("name", "DinoGo Product Index")).retrieve().body(JsonNode.class);
        String storeId = store == null ? "" : store.path("id").asText();
        if (storeId.isBlank()) throw new IllegalStateException("OpenAI did not return a Vector Store ID");
        return storeId;
    }

    List<ProductResponse> loadAllActiveProducts() {
        List<ProductResponse> products = new ArrayList<>();
        int pageNumber = 0;
        Page<ProductResponse> page;
        do {
            page = productService.getProducts(null, null, null, null, null, null, null, null,
                    pageNumber++, PRODUCT_PAGE_SIZE, "salesDesc");
            products.addAll(page.getContent());
        } while (page.hasNext());
        return products;
    }

    private List<IndexedFile> awaitAttachments(List<CompletableFuture<IndexedFile>> attachmentFutures) {
        try {
            CompletableFuture.allOf(attachmentFutures.toArray(CompletableFuture[]::new)).join();
            return attachmentFutures.stream().map(CompletableFuture::join).toList();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            log.warn("Semantic index file upload or attachment failed: {}", cause == null ? exception.getMessage() : cause.getMessage());
            return null;
        }
    }

    private IndexedFile uploadAndAttach(String storeId, ProductResponse product, Set<String> uploadedFileIds) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("purpose", "assistants");
        form.add("file", new ByteArrayResource(document(product).getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return "product-" + product.getProductId() + ".txt"; }
        });

        JsonNode file = restClient.post().uri("/files").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(JsonNode.class);
        String fileId = file == null ? "" : file.path("id").asText();
        if (fileId.isBlank()) throw new IllegalStateException("OpenAI did not return an uploaded file ID");
        uploadedFileIds.add(fileId);

        JsonNode attachment = restClient.post().uri("/vector_stores/{id}/files", storeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("file_id", fileId)).retrieve().body(JsonNode.class);
        String attachmentId = attachment == null ? "" : attachment.path("id").asText();
        if (attachmentId.isBlank()) throw new IllegalStateException("OpenAI did not return a Vector Store file ID");
        return new IndexedFile(fileId, attachmentId);
    }

    private String document(ProductResponse product) {
        return "productId: " + product.getProductId() + "\nname: " + product.getProductName() + "\ndescription: "
                + (product.getDescription() == null ? "" : product.getDescription()) + "\nprice: " + product.getMinPrice();
    }

    private String waitForAllCompletion(String storeId, List<String> attachmentIds) {
        return waitForAllCompletion(storeId, attachmentIds, Instant.now().plus(INDEX_TIMEOUT));
    }

    String waitForAllCompletion(String storeId, List<String> attachmentIds, Instant deadline) {
        Set<String> pendingIds = new LinkedHashSet<>(attachmentIds);

        while (!pendingIds.isEmpty() && Instant.now().isBefore(deadline)) {
            for (String attachmentId : new ArrayList<>(pendingIds)) {
                if (Duration.between(Instant.now(), deadline).compareTo(MAX_OPENAI_REQUEST_DURATION) < 0) return "timeout";
                JsonNode current = restClient.get().uri("/vector_stores/{storeId}/files/{fileId}", storeId, attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).retrieve().body(JsonNode.class);
                String status = current == null ? "failed" : current.path("status").asText("in_progress");
                if ("completed".equals(status)) pendingIds.remove(attachmentId);
                else if ("failed".equals(status) || "cancelled".equals(status)) return status;
            }

            if (!pendingIds.isEmpty()) {
                long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
                if (!sleep(Math.min(POLL_INTERVAL.toMillis(), remainingMillis))) return "interrupted";
            }
        }
        return pendingIds.isEmpty() ? "completed" : "timeout";
    }

    private SemanticIndexRebuildResponse failAndCleanup(VectorStoreStateService.VectorStoreState activeState,
            String storeId, Collection<String> fileIds, int productCount, String status) {
        if (storeId != null && !storeId.isBlank()) {
            List<VectorStoreStateService.CleanupTarget> pendingCleanup = new ArrayList<>(activeState.pendingCleanup());
            pendingCleanup.add(enrichCleanupTarget(new VectorStoreStateService.CleanupTarget(storeId, List.copyOf(fileIds))));
            VectorStoreStateService.VectorStoreState stateWithPendingCleanup = new VectorStoreStateService.VectorStoreState(
                    activeState.vectorStoreId(), activeState.fileIds(), pendingCleanup);
            try {
                vectorStoreStateService.save(stateWithPendingCleanup);
                retryPendingCleanup(stateWithPendingCleanup);
            } catch (Exception exception) {
                log.warn("Unable to persist pending OpenAI cleanup state: {}", exception.getMessage());
                cleanupStoreAndFiles(new VectorStoreStateService.CleanupTarget(storeId, List.copyOf(fileIds)));
            }
        }
        return new SemanticIndexRebuildResponse(storeId, productCount, status);
    }

    VectorStoreStateService.VectorStoreState retryPendingCleanup(VectorStoreStateService.VectorStoreState state) {
        List<VectorStoreStateService.CleanupTarget> remaining = new ArrayList<>();
        for (VectorStoreStateService.CleanupTarget target : state.pendingCleanup()) {
            VectorStoreStateService.CleanupTarget enrichedTarget = enrichCleanupTarget(target);
            if (!cleanupStoreAndFiles(enrichedTarget)) remaining.add(enrichedTarget);
        }
        if (remaining.equals(state.pendingCleanup())) return state;
        VectorStoreStateService.VectorStoreState updatedState = new VectorStoreStateService.VectorStoreState(
                state.vectorStoreId(), state.fileIds(), remaining);
        vectorStoreStateService.save(updatedState);
        return updatedState;
    }

    private VectorStoreStateService.CleanupTarget enrichCleanupTarget(VectorStoreStateService.CleanupTarget target) {
        Set<String> fileIds = new LinkedHashSet<>(target.fileIds());
        fileIds.addAll(listStoreFileIds(target.vectorStoreId()));
        return new VectorStoreStateService.CleanupTarget(target.vectorStoreId(), List.copyOf(fileIds));
    }

    private boolean cleanupStoreAndFiles(VectorStoreStateService.CleanupTarget target) {
        boolean successful = true;
        if (target.vectorStoreId() != null && !target.vectorStoreId().isBlank()) {
            try {
                restClient.delete().uri("/vector_stores/{id}", target.vectorStoreId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).retrieve().toBodilessEntity();
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 404) {
                    successful = false;
                    log.warn("Unable to delete Vector Store {} during cleanup: {}", target.vectorStoreId(), exception.getMessage());
                }
            } catch (Exception exception) {
                successful = false;
                log.warn("Unable to delete Vector Store {} during cleanup: {}", target.vectorStoreId(), exception.getMessage());
            }
        }
        for (String fileId : target.fileIds()) {
            try {
                restClient.delete().uri("/files/{id}", fileId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).retrieve().toBodilessEntity();
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 404) {
                    successful = false;
                    log.warn("Unable to delete OpenAI file {} during cleanup: {}", fileId, exception.getMessage());
                }
            } catch (Exception exception) {
                successful = false;
                log.warn("Unable to delete OpenAI file {} during cleanup: {}", fileId, exception.getMessage());
            }
        }
        return successful;
    }

    private Set<String> listStoreFileIds(String storeId) {
        if (storeId == null || storeId.isBlank()) return Set.of();
        try {
            JsonNode response = restClient.get().uri("/vector_stores/{id}/files?limit=100", storeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).retrieve().body(JsonNode.class);
            Set<String> fileIds = new LinkedHashSet<>();
            if (response != null) {
                for (JsonNode file : response.path("data")) {
                    String fileId = file.path("id").asText();
                    if (!fileId.isBlank()) fileIds.add(fileId);
                }
            }
            return fileIds;
        } catch (Exception exception) {
            log.warn("Unable to list OpenAI files for Vector Store {} during cleanup: {}", storeId, exception.getMessage());
            return Set.of();
        }
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

    private record IndexedFile(String fileId, String attachmentId) {}
}
