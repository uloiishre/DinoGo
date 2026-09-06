package com.dinogo.ai.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinogo.ai.dto.AiShoppingAdviceResponse;
import com.dinogo.ai.dto.AiShoppingCriteria;
import com.dinogo.ai.dto.AiShoppingComparison;
import com.dinogo.ai.dto.AiShoppingRecommendation;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.CategoryResponse;
import com.dinogo.catalog.service.CategoryService;
import com.dinogo.catalog.service.ProductService;

@Service
public class AiShoppingAdvisorService {
    private static final Logger log = LoggerFactory.getLogger(AiShoppingAdvisorService.class);
    private static final Pattern BUDGET = Pattern.compile(
            "(?:(?:預算|NT\\$|新台幣)\\s*(\\d{1,3}(?:,\\d{3})*|\\d{1,6})|(?<![A-Za-z0-9])(\\d{1,3}(?:,\\d{3})*|\\d{1,6})\\s*(?:元|塊))");
    private static final Pattern FALLBACK_INTENT = Pattern.compile(
            "^(?:我想購買|我要購買|幫我購買|我想買|想買|我要買|購買|我要|想要|請推薦(?:給我)?|推薦(?:給我)?|幫我(?:找|挑|推薦)?|請問)\\s*|"
                    + "\\s*(?:請推薦(?:給我)?|推薦(?:給我)?|給我推薦|幫我(?:找|挑|推薦)?|可以嗎|謝謝)$");
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ShoppingAdvisorLlmClient llmClient;
    private final ProductSemanticSearchService productSemanticSearchService;

    public AiShoppingAdvisorService(ProductService productService, CategoryService categoryService, ShoppingAdvisorLlmClient llmClient) {
        this(productService, categoryService, llmClient, null);
    }

    @Autowired
    public AiShoppingAdvisorService(ProductService productService, CategoryService categoryService, ShoppingAdvisorLlmClient llmClient, ProductSemanticSearchService productSemanticSearchService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.llmClient = llmClient;
        this.productSemanticSearchService = productSemanticSearchService;
    }

    public AiShoppingAdviceResponse advise(String message) {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        ShoppingAdvisorLlmClient.Result llmResult = llmClient.parse(message, categories);
        AiShoppingCriteria criteria = llmResult.criteria() == null ? parseCriteria(message)
                : sanitizeCriteria(llmResult.criteria(), categories);
        List<ProductResponse> products = findCandidates(criteria, message, categories);
        List<AiShoppingRecommendation> recommendations = products.stream()
                .map(product -> new AiShoppingRecommendation(product, reasons(product, criteria))).toList();
        String summary = recommendations.isEmpty() ? "目前沒有符合條件的已上架商品，請調整關鍵字或預算。" : criteria.intentSummary();
        return new AiShoppingAdviceResponse(summary, criteria, recommendations, createComparison(products),
                llmResult.generatedByAi(), llmResult.modelName());
    }

    private AiShoppingComparison createComparison(List<ProductResponse> products) {
        if (products.isEmpty()) return null;
        ProductResponse cheapest = products.stream().filter(product -> product.getMinPrice() != null)
                .min(java.util.Comparator.comparing(ProductResponse::getMinPrice)).orElse(products.get(0));
        ProductResponse highestStock = products.stream()
                .max(java.util.Comparator.comparing(product -> product.getStock() == null ? 0 : product.getStock()))
                .orElse(products.get(0));
        ProductResponse topSelling = products.stream()
                .max(java.util.Comparator.comparing(product -> product.getSoldCount() == null ? 0 : product.getSoldCount()))
                .orElse(products.get(0));
        return new AiShoppingComparison(cheapest.getProductName(), highestStock.getProductName(),
                topSelling.getProductName(), "價格、庫存與銷量各有取捨，請依你的優先條件選擇。");
    }

    private List<ProductResponse> findCandidates(AiShoppingCriteria criteria, String originalMessage,
            List<CategoryResponse> categories) {
        if (productSemanticSearchService != null) {
            Set<Integer> semanticIds = new java.util.LinkedHashSet<>(productSemanticSearchService.searchProductIds(originalMessage));
            if (!semanticIds.isEmpty()) {
                Map<Integer, ProductResponse> productsById = productService
                        .getProducts(null, criteria.categoryId(), null, null, null, null, criteria.maxPrice(), null, 0, 100, "salesDesc")
                        .getContent().stream().collect(java.util.stream.Collectors.toMap(ProductResponse::getProductId, product -> product));
                List<ProductResponse> semanticProducts = semanticIds.stream()
                        .map(productsById::get)
                        .filter(java.util.Objects::nonNull)
                        .filter(product -> product.getStock() != null && product.getStock() > 0)
                        .toList();
                semanticProducts = supplementExplicitCampingProducts(semanticProducts, originalMessage, criteria.maxPrice());
                log.info("Semantic product search resolved {} in-stock candidates", semanticProducts.size());
                if (!semanticProducts.isEmpty()) return semanticProducts;
            }
        }
        Page<ProductResponse> exactMatch = productService.getProducts(criteria.keyword(), criteria.categoryId(), null, null, null,
                null, criteria.maxPrice(), null, 0, 5, "salesDesc");
        if (!exactMatch.isEmpty()) {
            return exactMatch.getContent();
        }

        Map<Integer, ProductResponse> matches = new LinkedHashMap<>();
        for (String categoryTerm : categoryTerms(originalMessage)) {
            categories.stream()
                    .filter(category -> category.getCategoryName().contains(categoryTerm))
                    .findFirst()
                    .ifPresent(category -> productService.getProducts(null, category.getCategoryId(), null, null, null,
                            null, criteria.maxPrice(), null, 0, 5, "salesDesc")
                            .forEach(product -> matches.putIfAbsent(product.getProductId(), product)));
            if (!matches.isEmpty()) {
                return matches.values().stream().limit(5).toList();
            }
        }

        for (String term : candidateTerms(criteria.keyword())) {
            if (term.length() < 2) {
                continue;
            }
            productService.getProducts(term, null, null, null, null, null, criteria.maxPrice(), null, 0, 5, "salesDesc")
                    .forEach(product -> matches.putIfAbsent(product.getProductId(), product));
            if (matches.size() >= 5) {
                break;
            }
        }
        if (!matches.isEmpty()) {
            return matches.values().stream().limit(5).toList();
        }

        // A budget-only fallback is not semantically safe: it can recommend unrelated products.
        return List.of();
    }

    private List<ProductResponse> supplementExplicitCampingProducts(List<ProductResponse> semanticProducts,
            String originalMessage, BigDecimal maxPrice) {
        if (!originalMessage.contains("\u9732\u71df")) return semanticProducts;

        Map<Integer, ProductResponse> merged = new LinkedHashMap<>();
        semanticProducts.forEach(product -> merged.put(product.getProductId(), product));
        productService.getProducts("\u9732\u71df", null, null, null, null, null, maxPrice, null, 0, 100, "salesDesc")
                .filter(product -> product.getStock() != null && product.getStock() > 0)
                .forEach(product -> merged.putIfAbsent(product.getProductId(), product));
        return List.copyOf(merged.values());
    }

    private Set<String> candidateTerms(String keyword) {
        Set<String> terms = new java.util.LinkedHashSet<>();
        for (String term : keyword.split("[\\s,，、]+")) {
            if (term.length() >= 2) terms.add(term);
        }
        String compact = keyword.replaceAll("[\\s,，、。！？!?]", "");
        for (int length = 4; length >= 2; length--) {
            for (int start = 0; start + length <= compact.length(); start++) {
                terms.add(compact.substring(start, start + length));
            }
        }
        return terms;
    }

    private Set<String> categoryTerms(String keyword) {
        if (keyword.contains("零食")) {
            return Set.of("食品", "零食");
        }
        if (keyword.contains("3C") || keyword.contains("電子產品") || keyword.contains("電子")) {
            return Set.of("3C", "電子");
        }
        if (keyword.contains("衣服") || keyword.contains("服裝") || keyword.contains("穿搭")) {
            return Set.of("服飾");
        }
        if (keyword.contains("化妝品") || keyword.contains("彩妝") || keyword.contains("保養品")) {
            return Set.of("美妝", "保養");
        }
        return Set.of();
    }

    private AiShoppingCriteria parseCriteria(String message) {
        Matcher matcher = BUDGET.matcher(message);
        BigDecimal maxPrice = null;
        if (matcher.find()) {
            String amount = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            maxPrice = BigDecimal.valueOf(Long.parseLong(amount.replace(",", "")));
        }
        String keyword = message.replaceAll("(?:預算|NT\\$?|新台幣)?\\s*\\d{1,6}\\s*(?:元|塊)?", "")
                .replaceAll("[，,。！？!?]", " ").trim();
        keyword = FALLBACK_INTENT.matcher(keyword).replaceAll("").trim();
        if (keyword.length() > 40) keyword = keyword.substring(0, 40);
        return new AiShoppingCriteria(keyword, null, maxPrice, "依你的需求，從 DinoGo 已上架商品中挑出以下選項。");
    }

    private AiShoppingCriteria sanitizeCriteria(AiShoppingCriteria criteria, List<CategoryResponse> categories) {
        boolean isKnownCategory = criteria.categoryId() != null && categories.stream()
                .anyMatch(category -> category.getCategoryId().equals(criteria.categoryId()));
        return new AiShoppingCriteria(criteria.keyword(), isKnownCategory ? criteria.categoryId() : null,
                criteria.maxPrice(), criteria.intentSummary());
    }

    private List<String> reasons(ProductResponse product, AiShoppingCriteria criteria) {
        List<String> reasons = new ArrayList<>();
        if (criteria.maxPrice() != null && product.getMinPrice() != null && product.getMinPrice().compareTo(criteria.maxPrice()) <= 0)
            reasons.add("最低售價 NT$ " + product.getMinPrice() + "，符合預算上限");
        if (product.getStock() != null && product.getStock() > 0) reasons.add("目前庫存 " + product.getStock() + " 件");
        if (product.getSoldCount() != null && product.getSoldCount() > 0) reasons.add("已有 " + product.getSoldCount() + " 件銷售紀錄");
        if (reasons.isEmpty()) reasons.add("符合目前商品搜尋條件");
        return reasons;
    }
}
