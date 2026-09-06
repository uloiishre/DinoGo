package com.dinogo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.service.ProductService;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ProductSemanticIndexServiceTest {
    @Mock private ProductService productService;
    @Mock private VectorStoreStateService vectorStoreStateService;

    @Test
    void loadsEveryPageOfActiveProductsBeforeIndexing() {
        ProductResponse first = org.mockito.Mockito.mock(ProductResponse.class);
        ProductResponse last = org.mockito.Mockito.mock(ProductResponse.class);
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(100), eq("salesDesc")))
                .thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 100), 101));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), eq(1), eq(100), eq("salesDesc")))
                .thenReturn(new PageImpl<>(List.of(last), PageRequest.of(1, 100), 101));

        ProductSemanticIndexService service = new ProductSemanticIndexService(productService, "test-key", "", vectorStoreStateService,
                RestClient.builder().baseUrl("http://openai.test").build());

        assertEquals(List.of(first, last), service.loadAllActiveProducts());
    }

    @Test
    void keepsFailedCleanupPendingAndRemovesItAfterTheNextSuccessfulRetry() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductSemanticIndexService service = new ProductSemanticIndexService(productService, "test-key", "", vectorStoreStateService, builder.build());
        var target = new VectorStoreStateService.CleanupTarget("vs-old", List.of("file-old"));
        var state = new VectorStoreStateService.VectorStoreState("vs-active", List.of("file-active"), List.of(target));

        expectListThenCleanup(server, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(state, service.retryPendingCleanup(state));
        server.verify();

        server.reset();
        expectListThenCleanup(server, HttpStatus.OK, HttpStatus.OK);
        var result = service.retryPendingCleanup(state);
        assertEquals(List.of(), result.pendingCleanup());
        server.verify();
    }

    @Test
    void stopsPollingBeforeStartingARequestThatCannotFitWithinTheDeadline() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductSemanticIndexService service = new ProductSemanticIndexService(productService, "test-key", "", vectorStoreStateService, builder.build());

        assertEquals("timeout", service.waitForAllCompletion("vs-new", List.of("file-new"), Instant.now().plusSeconds(10)));
        server.verify();
    }

    private void expectListThenCleanup(MockRestServiceServer server, HttpStatus storeStatus, HttpStatus fileStatus) {
        server.expect(once(), requestTo("http://openai.test/vector_stores/vs-old/files?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"file-old\"}]}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://openai.test/vector_stores/vs-old"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(storeStatus));
        server.expect(once(), requestTo("http://openai.test/files/file-old"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(fileStatus));
    }
}
