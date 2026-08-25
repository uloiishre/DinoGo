# DinoGo Codex Project Rules

> 本文件是 DinoGo repository 的 Codex 入口規範。它只保留跨任務必須遵守的規則；操作細節以 `docs/` 中對應文件為準。

## 1. 專案與本機資訊

DinoGo 是 Java / Spring Boot、Vue 3、MSSQL 的六人 B2B2C 電商專案。

- 本文件不得記錄個人身分、實際進度、feature branch、帳密或本機老師專案路徑。
- 若存在 `AGENTS.local.md`，開始實作前一併讀取；它只能補充目前操作者的本機資訊，不能覆蓋本文件或正式 Source of Truth。
- `AGENTS.local.md`、`teacher-reference/`、`.env*`、本機設定與任何敏感資料不得提交。

## 2. 文件權威性

| 文件 | 用途 | 規則 |
| --- | --- | --- |
| `docs/database-schema.md` | 正式資料表、欄位、PK/FK、關聯、ownership | DB schema 唯一 Source of Truth；未經明確批准不得修改。 |
| `docs/team-plan.md` | 分工、時程、每日計畫 | 計畫不等於實際完成狀態；查詢進度時要對照 Git 與程式。 |
| `docs/git-workflow.md` | 分支、commit、push、PR、衝突處理 | 所有 Git 寫入操作前必讀。 |
| `docs/teacher-code-style.md` | 老師專案可沿用的 coding pattern | 先讀摘要；不足時才讀本機 read-only reference。 |
| `docs/README.md` | 文件索引與任務閱讀路由 | 需要多份文件時先讀此索引。 |

衝突時依序遵守：使用者當前明確需求、DB schema、team plan、git workflow、現有 codebase 一致性、本文件、teacher style、一般慣例。

## 3. 依任務最小讀取

不要每次讀遍全部文件。依任務只讀下列最小集合：

| 任務 | 必讀文件 |
| --- | --- |
| Git、commit、push、PR、分支 | `docs/git-workflow.md` |
| DB、Entity、JPA、Repository、SQL | `docs/database-schema.md`、`docs/teacher-code-style.md` |
| 功能 ownership、今日計畫、跨模組時程 | `docs/team-plan.md` |
| Backend、REST、Security、Validation 實作 | `docs/security-rules.md`、`docs/teacher-code-style.md` 與直接相關程式 |
| API 串接或跨模組 contract | `docs/README.md` 與直接相關 `*-api.md` / `*-cross-module-contracts.md` |
| Vue、UI、樣式、Router、Layout、Axios、Pinia、前端 build | `docs/design/frontend-task-rules.md`，再依其路由讀取最小文件 |

日常後端、DB、API、測試、Git、文件任務不得主動載入 `docs/design/`、設計參考圖或 Vue teacher reference。

## 4. 資料、模組與 API 邊界

- A：member、address、role、member_role、member_oauth_account。
- B：商品目錄；C：購物車與收藏；D：訂單／付款／物流；E：賣家中心；F：通知／評價／客服。完整表與 ownership 以 database schema 為準。
- 跨模組優先使用既有 API／Service contract，不以直接修改其他模組程式繞過依賴。
- 不得自行新增或刪除資料表、欄位、PK/FK、關聯、migration 或變更 ownership。
- API 若變更 URL、HTTP method、request/response 欄位或型別、validation、authorization / role，必須在實作前說明影響並通知相關模組。

## 5. 實作規則

- 維持 `Controller → Service → Repository → Database` 分層；Controller 不直接操作 Repository。
- DTO 是 API request/response 邊界；不要回傳密碼、Entity 或不必要的內部資料。
- 密碼使用 BCrypt、認證使用 JWT；不得 log plaintext password、完整 token、secret 或 API key。
- Entity mapping 必須符合 database schema；核心欄位明確指定 `@Table`、`@Column`、`@JoinColumn`。
- 最小修改：不順手重構、不全域格式化、不換 framework、不新增大型 dependency、不刪除可能被他人使用的程式。
- 老師 reference 只讀取與任務相符的部分，只學 pattern，不複製 business logic；衝突時維持 DinoGo 正式文件與現有架構。

## 6. 前端／設計按需載入規則

本規則是文件閱讀路由，不是外部執行 hook。

只有任務明確涉及 Vue 元件、頁面、樣式、RWD、UI/UX、設計稿、CSS、Bootstrap、Router、Layout、導覽、Axios、Pinia、前端 API 串接或前端 build 時，才讀：

```text
docs/design/frontend-task-rules.md
```

該文件決定後續要讀取哪些設計文件與是否需要 Vue teacher reference。不要一次掃描整個 `docs/design/`。

## 7. Git 安全

- 可直接做只讀 Git 分析：`status`、`branch`、`diff`、`log`、`fetch origin`。
- `commit`、push、merge、rebase、刪 branch、PR 需要使用者當次明確要求。
- 禁止自行使用 force push、`reset --hard`、`clean -fd`、`git init` 或刪除其他人的未提交修改。
- `develop` 是整合分支，功能開發使用 `feature/*`；有未提交修改時不可自行 switch、pull、merge 或 rebase。
- commit / push / PR 前，確認 branch、diff、測試、敏感檔案、無關檔案與 API/DB 影響。完整流程與格式以 `docs/git-workflow.md` 為準。

## 8. Task / Session 工作方式

- 工作流程以 Task 為單位，不以日期或單一 Codex session 為單位；一個 Task 可以跨多個 session。
- 開始或接續 Task 時，重新讀取 `AGENTS.md`、存在時的 `AGENTS.local.md`，並確認 branch、working tree、最近 commit、遠端差異、Task 目標與未完成事項。
- 不可假設前一個 session 的判斷或 working tree 狀態仍然正確；有未提交修改時，不自行 pull、switch、merge 或 rebase。
- Task 完成、暫停或準備切換 session 時，整理：Task 目標、已完成、目前修改、測試、Git 狀態、未完成／blocker 與下一步。

## 9. 修改前後的工作方式

### 修改前

涉及多檔案、Authentication/JWT、Security、DB mapping、共用架構、API contract、跨模組依賴或大規模重構時，先說明：

1. 需求理解與現有範圍。
2. 會修改的檔案與各自改動。
3. API、DB、其他模組與 breaking-change 影響。
4. 共用檔案風險與需通知對象。

使用者若明確要求直接執行，可直接進行已說明的範圍，但仍遵守最小修改原則。若使用者明確要求「先分析並判斷是否會碰共用檔案」，分析後必須等待確認才修改。

### 修改後

- 先 `git status`、`git diff`，確認沒有無關或敏感修改。
- Backend 依風險執行 compile 與相關 tests；若可能影響啟動，確認 Spring Boot 可啟動。
- Frontend 依風險執行 build、既有 lint/tests 與必要串接檢查。
- 不得為了讓 test 變綠而任意改其他模組。

完成時以以下格式回報：

```text
### Modified
### What changed
### Validation
### Cross-module impact
### Risks / Notes
```

## 10. 首次導入與文件維護

首次使用本規範時，先確認 repository、branch、working tree、`.gitignore`、正式文件與 teacher reference 是否被追蹤；只做分析，不修改 business logic、commit、push、merge 或 PR。

新增或更新文件時，先判斷它屬於 Source of Truth、工作規範、模組 API 或跨模組 contract。更新 `docs/README.md` 索引，不要把不同權威層級的內容複製到多個地方。
