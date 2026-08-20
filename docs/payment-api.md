# 付款 API

> D 訂單模組的付款 API contract。資料表與 enum 仍以 `database-schema.md` 為準。

所有 API 都需要有效 JWT。買家身分一律取自 JWT，request 不接受 `buyerId`；查不到該買家的訂單或付款時回傳 404，避免洩漏其他會員資料。

## 付款能力

```http
GET /api/payments/capabilities
```

成功回傳：

```json
{
  "simulationEnabled": false
}
```

前端必須依此 runtime 值決定是否顯示線上付款方式與呼叫模擬付款 API。`false` 時僅提供貨到付款；前後端都會拒絕建立線上付款，避免留下無法完成的 pending payment。

## 建立付款

```http
POST /api/orders/{orderId}/payments
Content-Type: application/json

{
  "paymentMethodCode": "CREDIT_CARD"
}
```

成功回傳 `201 Created` 與 `Location: /api/orders/{orderId}/payments/{paymentId}`。

- 金額由後端訂單總額決定，不接受前端金額。
- 模擬付款未啟用時，後端只接受 `CASH_ON_DELIVERY`；其他付款方式回傳驗證錯誤且不建立 payment。
- `CASH_ON_DELIVERY` 建立 `PENDING` 付款後，訂單直接進入 `PROCESSING`，不呼叫模擬付款結果 API。
- 線上付款建立時為 `PENDING`，再由 MVP 模擬付款結果 API 更新。
- 相同方式的 pending payment 重送會回傳既有付款；不同方式則回傳衝突錯誤。

## MVP 模擬付款結果

此 endpoint 預設關閉，只能在 Demo 環境明確啟用：

```text
SPRING_PROFILES_ACTIVE=demo
```

或設定：

```text
PAYMENT_SIMULATION_ENABLED=true
```

未啟用時回傳 `404 Not Found`，且不會更新付款或訂單。

```http
POST /api/orders/{orderId}/payments/{paymentId}/simulate
Content-Type: application/json

{
  "status": "SUCCESS",
  "failureReason": null
}
```

`status` 必填，且只接受：

- `SUCCESS`：付款改為成功、產生模擬交易編號，訂單由 `PENDING_PAYMENT` 進入 `PAID`。
- `FAILED`：付款改為失敗，訂單維持 `PENDING_PAYMENT`；可提供 `failureReason`。

成功回傳 `200 OK` 與付款資料。相同結果可安全重送；已完成的付款不可改成另一結果。

此 endpoint 僅供目前 MVP 模擬流程使用。非 Demo 環境必須維持關閉；串接正式金流後，應改由可信任的金流 callback/webhook 驗證交易結果，不得由瀏覽器自行宣告付款成功。

## Production 部署要求

- Production 必須維持 `PAYMENT_SIMULATION_ENABLED=false`，且不得使用 `demo` profile。
- 不得將模擬付款 endpoint 當作正式付款結果來源，也不得由瀏覽器提交付款成功狀態。
- 串接正式金流時，必須以金流供應商簽章驗證過的 callback/webhook 更新付款與訂單狀態；正式付款流程完成前，production 僅提供貨到付款。
