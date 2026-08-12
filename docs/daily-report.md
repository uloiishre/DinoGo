# DinoGo 專題每日回報紀錄本

本文件用來持續記錄 DinoGo 專題每天在 `C:\Users\User\Desktop\workspace` 工作時遇到的問題、排查過程、修正內容與測試結果，方便整理每日回報。

## 2026-08-12

專題：DinoGo

模組：E 賣家中心 / Coupon 優惠券

問題：Postman 測試 `POST /api/seller/coupons?sellerId=1` 新增優惠券時，回傳 `500 Internal Server Error`。

排查：查詢 SQL Server 的 `seller.Coupon` check constraints 後，確認資料庫 `scope_type` 允許值包含 `STORE`、`CATEGORY`、`PRODUCT`、`SUBCATEGORY`，但後端 `CouponService` 原本只允許 `ALL`、`CATEGORY`、`PRODUCT`，造成後端程式碼與資料庫 constraint 不一致。

修正：將後端 `CouponService` 的 scope type 驗證改為符合資料庫規則，修正 scope type 後端程式碼。

測試：修正後使用 Postman 測試新增優惠券成功。

回報摘要：今天修正 E 模組 Coupon 新增優惠券 500 錯誤，原因是 `scope_type` 後端驗證與資料庫限制不一致；已調整後端 scope type 規則並完成 Postman 測試。
