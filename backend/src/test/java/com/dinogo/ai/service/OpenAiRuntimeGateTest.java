package com.dinogo.ai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

class OpenAiRuntimeGateTest {

    @Test
    void advisorDoesNotSendRequestWhenDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ShoppingAdvisorLlmClient client = new ShoppingAdvisorLlmClient(org.mockito.Mockito.mock(ObjectMapper.class),
                "test-key", "", "test-model", false, builder.build());

        var result = client.parse("推薦露營燈", List.of());

        assertFalse(result.generatedByAi());
        server.verify();
    }

    @Test
    void semanticSearchDoesNotSendRequestWhenDisabled() {
        VectorStoreStateService stateService = org.mockito.Mockito.mock(VectorStoreStateService.class);
        when(stateService.getVectorStoreId()).thenReturn("vs-test");
        RestClient.Builder builder = RestClient.builder().baseUrl("http://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProductSemanticSearchService service = new ProductSemanticSearchService(stateService,
                org.mockito.Mockito.mock(ObjectMapper.class), "test-key", "", "test-model", false, builder.build());

        assertTrue(service.searchProductIds("露營燈").isEmpty());
        server.verify();
    }
}
