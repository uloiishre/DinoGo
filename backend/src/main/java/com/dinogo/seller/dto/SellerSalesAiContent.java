package com.dinogo.seller.dto;

public record SellerSalesAiContent(
        String trendAnalysis,
        String hotProductSuggestion,
        String couponSuggestion,
        String reminder) {

    public SellerSalesAiContent {
        if (trendAnalysis == null || trendAnalysis.isBlank()) {
            trendAnalysis = "目前資料仍有限，建議先持續累積訂單後再觀察銷售變化。";
        }
        if (hotProductSuggestion == null || hotProductSuggestion.isBlank()) {
            hotProductSuggestion = "優先確認熱賣商品庫存與商品資訊，避免有需求時無法出貨。";
        }
        if (couponSuggestion == null || couponSuggestion.isBlank()) {
            couponSuggestion = "可依目前銷售表現評估是否使用優惠券，提高買家下單意願。";
        }
        if (reminder == null || reminder.isBlank()) {
            reminder = "AI 內容僅根據目前後端彙整資料產生，請搭配實際訂單狀態判斷。";
        }
    }
}
