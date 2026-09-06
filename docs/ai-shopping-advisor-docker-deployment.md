# AI 購物顧問 Docker 部署交接

本文件提供 Docker 部署負責人交接使用，範圍僅包含 AI 購物顧問的環境設定、Vector Store 狀態保存、首次索引與驗證流程。

本功能不新增資料庫 schema，也不將商品資料或 OpenAI API Key 寫入 image 或 Git。

## 已納入 Compose 的設定

`compose.yml` 已將下列設定傳入 backend，並建立 named volume 保存索引狀態：

| 設定 | Docker 內預設值 | 用途 |
| --- | --- | --- |
| `OPENAI_API_KEY` | 空字串 | 呼叫 OpenAI、建立索引與語意搜尋所需的 Key。正式啟用時必填。 |
| `APP_AI_OPENAI_ENABLED` | `false` | OpenAI feature gate；只有設為 `true` 且 API Key 存在時，購物顧問才會呼叫 OpenAI 或建立索引。 |
| `APP_AI_OPENAI_MODEL` | `gpt-4.1-mini` | 需求解析使用的模型。請設定為帳號可用且支援目前 Responses 呼叫方式的模型。 |
| `AI_VECTOR_STORE_STATE_PATH` | `/app/data/ai-vector-store.json` | 保存目前 OpenAI Vector Store ID 的狀態檔位置。 |
| `ai_vector_store_data` | 掛載至 `/app/data` | Docker named volume；容器重啟後保留狀態檔。 |

`backend/Dockerfile` 已建立 `/app/data` 並交給非 root 的應用程式使用者寫入，因此不需額外修改 image。

## 部署前準備

1. 由 `.env.example` 建立本機或部署環境的 `.env`。
2. 補齊既有 Compose 必填設定，例如 MSSQL、資料庫帳密、JWT、密碼重設與 Google OAuth 設定。
3. 另外設定 AI 相關值：

```dotenv
OPENAI_API_KEY=請填入部署用的OpenAI_API_Key
APP_AI_OPENAI_ENABLED=true
APP_AI_OPENAI_MODEL=gpt-4.1-mini
AI_VECTOR_STORE_STATE_PATH=/app/data/ai-vector-store.json
```

請勿提交 `.env`、任何 API Key、`backend/data/`，或 `ai-vector-store.json`。狀態檔不含 API Key，但記錄了部署環境使用的外部 Vector Store ID，應視為部署期設定。

## 啟動與確認

在 repository 根目錄、確認 `.env` 已備妥後執行。Cloudflare Tunnel Server 必須使用既有 overlay；不要只執行 base Compose，避免 SQL Server 產生非預期的 host exposure。

### Tunnel-only Server

```bash
docker compose -p dinogo \
  -f compose.yml \
  -f compose.tunnel.yml \
  config -q

docker compose -p dinogo \
  -f compose.yml \
  -f compose.tunnel.yml \
  up -d --build

docker compose -p dinogo \
  -f compose.yml \
  -f compose.tunnel.yml \
  ps
```

### Tunnel + Team LAN SQL Server Server

只有需要提供團隊 LAN SQL Server access 的 Server，才加入 `compose.team.yml`：

```bash
docker compose -p dinogo \
  -f compose.yml \
  -f compose.tunnel.yml \
  -f compose.team.yml \
  config -q

docker compose -p dinogo \
  -f compose.yml \
  -f compose.tunnel.yml \
  -f compose.team.yml \
  up -d --build

docker compose -p dinogo \
  -f compose.yml \
  -f compose.tunnel.yml \
  -f compose.team.yml \
  ps
```

Team mode 只允許 `${MSSQL_BIND_IP}:${MSSQL_HOST_PORT:-9434}:1433`，不得使用公開 host 1433。

`docker compose config` 可先確認環境變數是否已被展開；不要將其輸出貼到公開管道，避免意外暴露 secret。

## 首次建立商品語意索引

啟動完成後，必須由管理者建立索引；一般使用者不能呼叫此 API。

1. 使用既有 `POST /api/auth/login` 登入管理者帳號，取得 JWT。
2. 在 Postman 或 API Client 呼叫：

```http
POST /api/admin/ai-shopping-advisor/index/rebuild
Authorization: Bearer <管理者 JWT>
```

此 API 不需要 request body。成功完成時會回傳：

```json
{
  "vectorStoreId": "vs_...",
  "indexedProductCount": 29,
  "status": "completed"
}
```

只有 `status` 為 `completed` 時，後端才會將 `vectorStoreId` 寫到 `/app/data/ai-vector-store.json`。之後一般使用者的購物顧問請求才能使用商品語意搜尋。

重建時會逐頁讀取所有目前已上架商品，每個商品建立獨立索引文件、平行上傳與附加，再以整體 polling 等待完成。請等待 API 回應，不要同時重複送出重建請求。

> 注意：每次重建會先建立新的 OpenAI Vector Store；新索引完成時，程式會先將舊資源寫入狀態檔的 `pendingCleanup`，再切換 active Store ID。只有刪除成功才會移除 pending 記錄；下次 rebuild 會先重試。重建失敗、逾時或中斷時，也會將新建資源加入 pending cleanup。若狀態檔本身無法寫入，程式會改為立即嘗試清理並記錄 warning，部署人員應查看 backend log。

## 重啟與持久化行為

Compose 的 `ai_vector_store_data` named volume 掛載到 `/app/data`。因此正常的 `docker compose restart`、容器重建或 `docker compose up -d --build` 後，狀態檔仍會存在，無需重新建立索引。

若發生以下任一情況，需以管理者重新呼叫 rebuild API：

- 第一次部署尚未建立索引。
- `ai_vector_store_data` volume 被刪除或改用新的 Compose project / volume。
- 狀態檔遺失、損毀，或其中的 Vector Store 已在 OpenAI 端被刪除。
- 商品資料有實質更新，需更新語意索引。

早期狀態檔只含 `vectorStoreId`、不含 `fileIds`。第一次以新版程式重建時，程式會先向舊 Store 列出檔案後再清理；新版狀態檔也會保存 file IDs，讓清理在列舉 API 暫時失敗時仍有可用紀錄。

不要將主機上的狀態檔手動複製進 Docker image，也不要以 bind mount 覆蓋 `/app/data`，除非部署團隊已規劃好等效的持久化與備份策略。

## 功能驗證

以一般已登入使用者的 JWT 呼叫：

```http
POST /api/ai-shopping-advisor
Authorization: Bearer <一般使用者 JWT>
Content-Type: application/json

{
  "message": "想找適合戶外過夜使用的照明用品"
}
```

確認回應中的 `generatedByAi` 為 `true`，並檢查推薦商品皆來自 DinoGo 真實商品資料。可再以不同預算、分類與無結果需求各測一組。

商品候選會再依目前上架狀態、庫存、預算與分類條件過濾；因此 Vector Store 有命中不代表一定會出現在最終回應。

## 排錯對照

| 現象 | 優先檢查 |
| --- | --- |
| 顧問沒有語意搜尋結果 | `OPENAI_API_KEY` 是否已傳入 backend、`/app/data/ai-vector-store.json` 是否存在，以及其中的 Vector Store 是否仍有效。 |
| rebuild 回傳非 `completed` | 查看 backend log 與 OpenAI API 回應；確認 Key、額度、網路連線與模型權限。程式會嘗試清理本次新建資源。 |
| 推薦為空 | 確認資料庫中是否有已上架、庫存大於 0、符合預算 / 分類的商品；必要時重新建立索引。 |
| 重啟後失去語意搜尋 | 檢查 `ai_vector_store_data` named volume 是否仍掛載到 `/app/data`，以及是否更換了 Compose project 名稱。 |
| 同一使用者收到 `429` | 目前 API 有每位使用者每分鐘 10 次的限制；等待一分鐘再測試。 |

後端日誌可用下列指令查看：

Tunnel-only Server：

```bash
docker compose -p dinogo -f compose.yml -f compose.tunnel.yml logs -f backend
```

Tunnel + Team LAN SQL Server Server：

```bash
docker compose -p dinogo -f compose.yml -f compose.tunnel.yml -f compose.team.yml logs -f backend
```

關鍵訊息包括 `Semantic product search skipped`、`Semantic product search failed`、`AI shopping criteria parsing failed` 與索引 API 的錯誤回應。

## 部署交接清單

- `.env` 已由部署環境安全注入，未加入 Git。
- Backend 容器已取得 `OPENAI_API_KEY`。
- `ai_vector_store_data` named volume 已建立並持久化。
- 管理者 rebuild API 已回傳 `status: completed`。
- 一般使用者 API 已實測 `generatedByAi: true`。
- 已記錄 OpenAI 資源管理與舊 Vector Store 清理責任人。
