# DinoGo 文件索引

本目錄以「單一文件、單一權威用途」管理。需要資訊時，先依任務找正確文件；不要把內容複製到多份規範中。

## 文件分類與權威性

| 類型 | 文件 | 用途 |
| --- | --- | --- |
| Source of Truth | [database-schema.md](database-schema.md) | 正式資料表、欄位、PK/FK、關聯、ownership、DDL。 |
| Source of Truth | [team-plan.md](team-plan.md) | 模組分工、每日計畫、時程與驗收。 |
| Source of Truth | [git-workflow.md](git-workflow.md) | 分支、commit、push、PR、衝突與交付流程。 |
| 工作規範 | [teacher-code-style.md](teacher-code-style.md) | 老師教材中可重複使用的 coding pattern。 |
| 模組 API | [member-api.md](member-api.md) | A 會員、認證與地址 API 的 request、response、錯誤格式與 JWT 規則。 |
| 模組 API | [payment-api.md](payment-api.md) | D 付款建立、MVP 模擬結果、狀態與 JWT 規則。 |
| 跨模組 contract | [order-cross-module-contracts.md](order-cross-module-contracts.md) | D 與 B、E 的訂單整合需求。 |
| 前端規範 | [pinia-guide.md](pinia-guide.md) | Pinia、JWT、Axios、Store、路由守衛、手動測試與組員新增規則。 |
| 設計入口 | [design/frontend-task-rules.md](design/frontend-task-rules.md) | 前端任務的最小設計文件閱讀路由。 |

## 任務閱讀路由

| 任務 | 先讀 | 視需要再讀 |
| --- | --- | --- |
| DB、Entity、JPA、Repository、SQL | `database-schema.md` | `teacher-code-style.md` |
| 功能分工、今日計畫、驗收 | `team-plan.md` | 直接相關程式與 Git 實際狀態 |
| Git、commit、push、PR | `git-workflow.md` | `AGENTS.md` 的安全邊界 |
| Spring REST、Validation、Security、JWT | `teacher-code-style.md` | 模組 API 文件、直接相關程式 |
| A 會員／登入／地址串接 | `member-api.md` | `database-schema.md`、直接相關 Controller／DTO |
| D 付款 API 串接 | `payment-api.md` | `database-schema.md`、直接相關 Controller／DTO |
| D、B、E 訂單整合 | `order-cross-module-contracts.md` | 相關模組 API／Service |
| Vue、UI、Router、Axios、Pinia、前端 build | `pinia-guide.md` | `design/frontend-task-rules.md` 及其指定的設計文件 |

## 文件維護規則

- DB 結構只更新 `database-schema.md`；未經明確批准不得修改。
- 分工與時程只更新 `team-plan.md`；計畫不等於實際完成狀態。
- Git 操作細節只更新 `git-workflow.md`。
- 模組 API 改變 URL、HTTP method、request/response 欄位或型別、validation、authorization / role 時，更新對應 `*-api.md` 並通知受影響模組。
- 跨模組協議使用 `*-cross-module-contracts.md`；不要把它混入單一模組 API 文件。
- 新增文件後，將它加入本索引；設計文件只在前端任務中按需讀取。
