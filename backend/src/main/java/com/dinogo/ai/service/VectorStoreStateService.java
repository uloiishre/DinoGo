package com.dinogo.ai.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class VectorStoreStateService {
    private final Path statePath;
    private final ObjectMapper objectMapper;
    public VectorStoreStateService(@Value("${app.ai.vector-store-state-path:${AI_VECTOR_STORE_STATE_PATH:./data/ai-vector-store.json}}") String statePath, ObjectMapper objectMapper) {
        this.statePath = Path.of(statePath); this.objectMapper = objectMapper;
    }
    public synchronized void save(String vectorStoreId) {
        try {
            Path parent = statePath.getParent(); if (parent != null) Files.createDirectories(parent);
            objectMapper.writeValue(statePath.toFile(), Map.of("vectorStoreId", vectorStoreId, "indexedAt", Instant.now().toString()));
        } catch (IOException exception) { throw new IllegalStateException("Unable to persist Vector Store state", exception); }
    }
    public synchronized String getVectorStoreId() {
        try {
            if (!Files.exists(statePath)) return null;
            JsonNode state = objectMapper.readTree(statePath.toFile());
            return state.path("vectorStoreId").asText(null);
        } catch (RuntimeException exception) { throw new IllegalStateException("Unable to read Vector Store state", exception); }
    }
}
