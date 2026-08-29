package com.dinogo.seller.service;

import java.util.List;
import java.util.Map;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dinogo.seller.dto.SellerSalesAiContent;
import com.dinogo.seller.dto.SellerSalesInsightStats;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class SalesInsightLlmClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public SalesInsightLlmClient(
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key:}") String configuredApiKey,
            @Value("${OPENAI_API_KEY:}") String environmentApiKey,
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String model,
            @Value("${app.ai.openai.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
        this.apiKey = configuredApiKey == null || configuredApiKey.isBlank()
                ? environmentApiKey
                : configuredApiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public LlmResult generateInsight(SellerSalesInsightStats stats) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return new LlmResult(createFallbackInsight(stats), false, null);
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(createRequest(stats))
                    .retrieve()
                    .body(JsonNode.class);

            String outputText = extractOutputText(response);
            SellerSalesAiContent content = objectMapper.readValue(outputText, SellerSalesAiContent.class);
            return new LlmResult(content, true, model);
        } catch (Exception exception) {
            return new LlmResult(createFallbackInsight(stats), false, null);
        }
    }

    private Map<String, Object> createRequest(SellerSalesInsightStats stats) throws Exception {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of(
                        "trendAnalysis",
                        "hotProductSuggestion",
                        "couponSuggestion",
                        "reminder"),
                "properties", Map.of(
                        "trendAnalysis", Map.of("type", "string"),
                        "hotProductSuggestion", Map.of("type", "string"),
                        "couponSuggestion", Map.of("type", "string"),
                        "reminder", Map.of("type", "string")));
        Map<String, Object> textFormat = Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "seller_sales_insight",
                        "strict", true,
                        "schema", schema));

        return Map.of(
                "model", model,
                "store", false,
                "temperature", 0.2,
                "max_output_tokens", 400,
                "instructions", """
                        你是電商賣家中心的銷售分析助理。
                        只能根據使用者提供的後端統計 JSON 撰寫繁體中文洞察。
                        不得自行產生、推測或改寫任何銷售數字、金額、百分比。
                        輸出內容不要包含具體數字，數字會由系統另外顯示。
                        銷售趨勢分析限 1 到 2 句，其餘欄位各限 1 句。
                        不做銷售預測，不提 RAG、Embedding、Vector DB。
                        """,
                "input", "請根據以下統計資料產生精簡的銷售趨勢分析、熱銷商品建議、優惠券建議與小提醒：\n"
                        + objectMapper.writeValueAsString(stats),
                "text", textFormat);
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalArgumentException("Empty LLM response");
        }

        String outputText = response.path("output_text").asText();
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                String text = contentItem.path("text").asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }

        throw new IllegalArgumentException("LLM response does not contain text output");
    }

    private SellerSalesAiContent createFallbackInsight(SellerSalesInsightStats stats) {
        if (stats.orderCount() == 0) {
            return new SellerSalesAiContent(
                    "本期尚未累積有效訂單，銷售狀況仍需要更多資料判斷。",
                    "先檢查上架商品曝光、商品圖片與價格設定，讓買家更容易完成下單。",
                    "目前可先保留優惠券資源，等有更多瀏覽或訂單訊號後再投放。",
                    "資料不足時不要急著解讀趨勢，先確保商品資訊完整。");
        }

        if (stats.revenueChangeRate().signum() < 0 || stats.orderCountChangeRate().signum() < 0) {
            return new SellerSalesAiContent(
                    "本期銷售表現較前期轉弱，建議優先確認流量、商品吸引力與出貨節奏。",
                    "檢查熱賣商品是否仍有足夠庫存與清楚的商品資訊。",
                    "可針對轉弱商品或高瀏覽低轉換商品安排小額優惠券，提高下單意願。",
                    "優先處理已付款與待出貨訂單，避免體驗問題放大銷售下滑。");
        }

        return new SellerSalesAiContent(
                "本期銷售表現維持穩定，可持續觀察熱賣商品與訂單處理效率。",
                "優先補足熱賣商品庫存，降低缺貨造成的銷售中斷。",
                "優惠券可集中在低銷或需要帶動回購的商品，避免折扣過度分散。",
                "維持待出貨訂單處理速度，讓買家體驗更穩定。");
    }

    public record LlmResult(
            SellerSalesAiContent content,
            boolean generatedByAi,
            String modelName) {
    }
}
