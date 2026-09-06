package com.dinogo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.CategoryResponse;
import com.dinogo.ai.dto.AiShoppingCriteria;
import com.dinogo.catalog.service.CategoryService;
import com.dinogo.catalog.service.ProductService;

@ExtendWith(MockitoExtension.class)
class AiShoppingAdvisorServiceTest {
    @Mock private ProductService productService;
    @Mock private CategoryService categoryService;
    @Mock private ShoppingAdvisorLlmClient llmClient;
    @Mock private ProductSemanticSearchService productSemanticSearchService;

    @Test
    void returnsAnExactSearchMatchWithoutCallingOpenAi() {
        ProductResponse product = product(1, "露營收納箱", 899);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(product)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("露營收納 預算 2000 元");

        assertFalse(response.generatedByAi());
        assertEquals(1, response.recommendations().size());
        assertEquals(1, response.recommendations().get(0).product().getProductId());
    }

    @Test
    void retriesWithIndividualTermsWhenTheFullPhraseDoesNotMatch() {
        ProductResponse product = product(2, "露營桌", 1200);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty())
                .thenReturn(new PageImpl<>(java.util.List.of(product)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("露營 耐用 預算 2000 元");

        assertEquals(1, response.recommendations().size());
        assertEquals("露營桌", response.recommendations().get(0).product().getProductName());
    }

    @Test
    void doesNotRecommendUnrelatedProductsWhenNoKeywordMatches() {
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty())
                .thenReturn(Page.empty());

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("不存在的需求 預算 2000 元");

        assertEquals(0, response.recommendations().size());
    }

    @Test
    void removesShoppingIntentWordsBeforeFallbackSearching() {
        ProductResponse product = product(3, "shampoo", 550);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(product)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("我要買洗髮精，推薦給我");

        assertEquals("洗髮精", response.criteria().keyword());
        assertEquals(1, response.recommendations().size());
    }

    @Test
    void removesPurchaseIntentWordsBeforeFallbackSearching() {
        ProductResponse product = product(6, "camping light", 499);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(product)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("我要購買露營燈，推薦給我");

        assertEquals("露營燈", response.criteria().keyword());
        assertEquals(1, response.recommendations().size());
    }

    @Test
    void searchesChinesePhrasesInsideAFullSentence() {
        ProductResponse product = product(7, "camping light", 499);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty())
                .thenReturn(new PageImpl<>(java.util.List.of(product)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient)
                .advise("喜歡露營的朋友生日，要買露營相關的商品給她當禮物");

        assertEquals(1, response.recommendations().size());
    }

    @Test
    void keepsOnlyInStockSemanticCandidatesWithinBudget() {
        ProductResponse inStock = product(8, "camping light", 499);
        ProductResponse outOfStock = product(9, "camping lantern", 399);
        outOfStock.setStock(0);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productSemanticSearchService.searchProductIds(any())).thenReturn(java.util.List.of(8, 9));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(inStock, outOfStock)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient, productSemanticSearchService)
                .advise("camping light 500 元內");

        assertEquals(1, response.recommendations().size());
        assertEquals(8, response.recommendations().get(0).product().getProductId());
    }

    @Test
    void preservesSemanticSearchOrderInsteadOfProductSalesOrder() {
        ProductResponse morePopular = product(8, "camping light", 499);
        ProductResponse moreRelevant = product(9, "camping lantern", 399);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productSemanticSearchService.searchProductIds(any())).thenReturn(java.util.List.of(9, 8));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(morePopular, moreRelevant)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient, productSemanticSearchService)
                .advise("camping light");

        assertEquals(9, response.recommendations().get(0).product().getProductId());
        assertEquals(8, response.recommendations().get(1).product().getProductId());
    }

    @Test
    void supplementsSemanticCampingCandidatesWithExactCampingProducts() {
        ProductResponse semanticProduct = product(8, "camping storage", 499);
        ProductResponse campingLight = product(9, "camping lantern", 399);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productSemanticSearchService.searchProductIds(any())).thenReturn(java.util.List.of(8));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(semanticProduct)))
                .thenReturn(new PageImpl<>(java.util.List.of(campingLight)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient, productSemanticSearchService)
                .advise("\u9732\u71df\u7528\u54c1");

        assertEquals(2, response.recommendations().size());
        assertEquals(8, response.recommendations().get(0).product().getProductId());
        assertEquals(9, response.recommendations().get(1).product().getProductId());
    }

    @Test
    void maps3cRequestsToTheExisting3cCategory() {
        ProductResponse product = product(4, "藍牙耳機", 1999);
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(null, false, null));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty())
                .thenReturn(new PageImpl<>(java.util.List.of(product)));
        when(categoryService.getAllCategories()).thenReturn(java.util.List.of(new CategoryResponse(1, "3C 電子")));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("想買 3C產品，2000 元內");

        assertEquals(1, response.recommendations().size());
        assertEquals("藍牙耳機", response.recommendations().get(0).product().getProductName());
        assertEquals(new BigDecimal("2000"), response.criteria().maxPrice());
    }

    @Test
    void usesOnlyCategoryIdsProvidedByTheDynamicCategoryList() {
        ProductResponse product = product(5, "charger", 599);
        when(categoryService.getAllCategories()).thenReturn(java.util.List.of(new CategoryResponse(1, "electronics")));
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(
                new AiShoppingCriteria("charger", 1, new BigDecimal("2000"), "recommendation"), true,
                "gpt-4.1-mini"));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty())
                .thenReturn(new PageImpl<>(java.util.List.of(product)));

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("charger");

        assertEquals(1, response.criteria().categoryId());
        assertEquals(1, response.recommendations().size());
        assertEquals(true, response.generatedByAi());
        verify(productService).getProducts(eq("charger"), eq(1), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void discardsAnAiCategoryIdThatIsNotInTheDynamicCategoryList() {
        when(categoryService.getAllCategories()).thenReturn(java.util.List.of(new CategoryResponse(1, "electronics")));
        when(llmClient.parse(any(), any())).thenReturn(new ShoppingAdvisorLlmClient.Result(
                new AiShoppingCriteria("charger", 999, null, "recommendation"), true, "gpt-4.1-mini"));
        when(productService.getProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        var response = new AiShoppingAdvisorService(productService, categoryService, llmClient).advise("charger");

        assertEquals(null, response.criteria().categoryId());
    }

    private ProductResponse product(int id, String name, int price) {
        return new ProductResponse(id, 1, 1, 1, name, "", BigDecimal.valueOf(price), BigDecimal.valueOf(price),
                BigDecimal.valueOf(price), 5, null, (byte) 1, 10);
    }
}
