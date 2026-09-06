package com.dinogo.ai.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
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
    public synchronized void save(String vectorStoreId, List<String> fileIds) {
        save(new VectorStoreState(vectorStoreId, fileIds, List.of()));
    }
    public synchronized void save(VectorStoreState state) {
        try {
            Path parent = statePath.getParent(); if (parent != null) Files.createDirectories(parent);
            Path temporaryPath = statePath.resolveSibling(statePath.getFileName() + ".tmp");
            objectMapper.writeValue(temporaryPath.toFile(), Map.of("vectorStoreId", state.vectorStoreId(),
                    "fileIds", state.fileIds(), "pendingCleanup", state.pendingCleanup(), "indexedAt", Instant.now().toString()));
            try {
                Files.move(temporaryPath, statePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, statePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) { throw new IllegalStateException("Unable to persist Vector Store state", exception); }
    }
    public synchronized void save(String vectorStoreId) { save(vectorStoreId, List.of()); }
    public synchronized String getVectorStoreId() {
        return getState().vectorStoreId();
    }
    public synchronized VectorStoreState getState() {
        try {
            if (!Files.exists(statePath)) return new VectorStoreState(null, List.of(), List.of());
            JsonNode state = objectMapper.readTree(statePath.toFile());
            List<String> fileIds = new java.util.ArrayList<>();
            for (JsonNode fileId : state.path("fileIds")) {
                String value = fileId.asText();
                if (!value.isBlank()) fileIds.add(value);
            }
            List<CleanupTarget> pendingCleanup = new java.util.ArrayList<>();
            for (JsonNode pending : state.path("pendingCleanup")) {
                String vectorStoreId = pending.path("vectorStoreId").asText();
                if (vectorStoreId.isBlank()) continue;
                List<String> pendingFileIds = new java.util.ArrayList<>();
                for (JsonNode fileId : pending.path("fileIds")) {
                    String value = fileId.asText();
                    if (!value.isBlank()) pendingFileIds.add(value);
                }
                pendingCleanup.add(new CleanupTarget(vectorStoreId, pendingFileIds));
            }
            return new VectorStoreState(state.path("vectorStoreId").asText(null), fileIds, pendingCleanup);
        } catch (RuntimeException exception) { throw new IllegalStateException("Unable to read Vector Store state", exception); }
    }
    public record VectorStoreState(String vectorStoreId, List<String> fileIds, List<CleanupTarget> pendingCleanup) {
        public VectorStoreState { fileIds = List.copyOf(fileIds); pendingCleanup = List.copyOf(pendingCleanup); }
    }
    public record CleanupTarget(String vectorStoreId, List<String> fileIds) {
        public CleanupTarget { fileIds = List.copyOf(fileIds); }
    }
}
