# DinoGo Codex Project Rules

> 本文件是 DinoGo 全組共用的 Codex 專案級開發規範，可提交至 GitHub。
> 本文件不得假設目前操作者是 A/B/C/D/E/F 中任何特定組員；Codex 應依當前任務、分支與 `docs/team-plan.md` 判斷工作範圍。

---

## 1. 專案定位

DinoGo 為 6 人協作開發的 B2B2C 電商平台。

主要技術：

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- RESTful API
- Maven
- JWT
- BCrypt
- Microsoft SQL Server

### Frontend
- Vue 3
- Vite
- JavaScript
- Bootstrap 5
- Axios

### Database
- Microsoft SQL Server

### 個人本機規範

全組共用規範不得寫入個人身分、個人今日進度、個人 feature branch 或本機老師教材路徑。

若本機存在：

```text
AGENTS.local.md
```

Codex 應在開始實作前一併閱讀，將其視為目前操作者的本機補充資訊。

`AGENTS.local.md`：
- 不提交 GitHub
- 不取代本 `AGENTS.md`
- 不得修改全組 Source of Truth
- 僅可補充目前操作者的模組、分支、個人進度與本機 reference 路徑

---

## 2. 正式規範文件

Codex 執行任務前，依任務內容閱讀下列文件：

```text
docs/database-schema.md
docs/team-plan.md
docs/git-workflow.md
docs/teacher-code-style.md
```

若 `docs/teacher-code-style.md` 尚未建立，先從 `teacher-reference/` 中與目前任務相關的老師專案抽取 coding conventions，再建立該文件。

---

## 3. Source of Truth 與衝突處理

不同文件負責不同層級，請勿把所有文件視為同一種規範。

### 3.1 資料庫結構與資料表 ownership

以：

```text
docs/database-schema.md
```

為唯一 Source of Truth。

包含：
- Table
- Column
- PK / FK
- Relationship
- SQL Schema
- Index / Constraint
- Seed Data
- MSSQL DDL
- 各資料表負責模組

除非使用者明確要求，不得自行：
- 新增或刪除資料表
- 修改 table 名稱
- 修改 column 名稱
- 修改 PK / FK
- 修改 relationship
- 修改 SQL Schema
- 改變正式資料表 ownership

若 Java Entity 與 database schema 不一致，先指出差異，不要自行修改 database schema 配合程式。

### 3.2 功能分工、每日進度與開發時程

以：

```text
docs/team-plan.md
```

為主。

此文件負責：
- A-F 功能分工
- 每日進度
- MVP 時程
- 跨模組整合需求
- 每日驗收標準

### 3.3 Git 協作規則

以：

```text
docs/git-workflow.md
```

為主。

### 3.4 已知文件差異

`team-plan.md` 的部分早期資料表分工與最新版 `database-schema.md` 不一致。

發生衝突時：

- 「功能由誰做」：看 `team-plan.md`
- 「資料表由哪一模組負責」：看 `database-schema.md`
- 「Git 怎麼操作」：看 `git-workflow.md`

不得為了讓文件彼此一致，而自行改動資料庫 ownership 或 business scope。

---

## 4. 最新資料表責任

資料表責任以 `docs/database-schema.md` 為準。

### A：會員與帳號模組

```text
member
address
role
member_role
member_oauth_account
```

### B：商品目錄模組

```text
category
subcategory
brand
product
product_sku
product_image
```

### C：購物車與收藏模組

```text
cart
cart_item
favorite
```

### D：訂單、付款與物流模組

```text
orders
order_item
payment_method
payment
shipment
```

### E：賣家中心模組

```text
seller
coupon
member_coupon
seller_ai_sales_analysis
```

### F：通知、評價與客服模組

```text
msg_template
msg_sample
msg
msg_recipient
product_record
member_record
service.role
service.topic
service.subtheme
service.demand
service.reply
```

### 全體組員暫定共同負責

```text
ai_conversation
```

目前正式資料模型共 35 張表。

---

## 5. 功能分工原則

功能開發以 `docs/team-plan.md` 為主，但若其中列出的「主要資料表」與 `database-schema.md` 不一致，以後者為準。

例如：

- C 仍負責購物流程中的「優惠券套用」功能，但 `coupon`、`member_coupon` 的正式資料表 ownership 屬於 E。
- F 的早期規劃可能出現 ChatRoom / ChatMessage / CustomerServiceTicket；若最新版 database schema 未定義這些表，不得自行建立，必須先依現行 F 模組的 `msg_*`、`product_record`、`member_record`、`service.*` 設計確認實作方式。
- E 不可自行新增 `SellerProfile` 或 `SellerApplication`，除非使用者明確批准 database schema 變更。

跨模組實作時，優先透過既有 API / Service contract 整合，不要直接修改其他組員模組以繞過依賴。

---

## 6. 老師專案作為 Code Style Reference

老師教材屬於本機 reference，不要求提交至 GitHub。

共同規範只定義 reference 的使用方式；實際本機路徑由 `AGENTS.local.md` 指定。

若本機有 `teacher-reference/`，一律僅供讀取。

### 可參考內容
- package structure
- class / method / variable naming
- Controller 寫法
- Service 寫法
- Repository / DAO 寫法
- Entity / Bean / DTO 使用方式
- JPA annotations
- relationship mapping
- dependency injection
- exception handling
- REST API design
- API response style
- Spring Security
- JWT authentication
- validation
- logging
- comments / formatting

### 禁止
- 修改老師原始專案
- 刪除老師檔案
- refactor 老師專案
- commit 老師專案
- 把老師原始碼加入 DinoGo repository
- 無判斷地複製整個 class 或 business logic

第一次分析後，將可重複使用的規則整理至：

```text
docs/teacher-code-style.md
```

日後優先讀這份整理，不要每次重新掃描所有老師專案。

---

### 6.1 Teacher Reference Project Map

老師參考專案依用途分成以下類型；實際檔案位置由各組員自己的 `AGENTS.local.md` 指定。

| 類型 | 主要用途 | 優先學習內容 |
| --- | --- | --- |
| Hibernate | Hibernate 基礎 | DAO / DAO implementation、Entity/Bean、Hibernate CRUD、Session/Transaction、Service 分層 |
| Spring Boot + AJAX / REST | 後端 API | Controller、Service、Repository、REST API、JSON/AJAX、前後端資料交換 |
| Vue | 前端串接 | Vue 3、Axios、Router、Store、component/view 結構、前後端 API 串接 |
| Java / Spring Security | 認證授權 | BCrypt、Filter、Interceptor、登入驗證、Security 設定、JWT、安全性分層 |

#### Reference 選擇規則

- Hibernate / JPA / DAO 任務：只讀 Hibernate reference。
- Spring Boot Controller / Service / Repository / REST / AJAX 任務：只讀 Spring REST/AJAX reference。
- Vue / Axios / Router / 前後端串接任務：只讀 Vue reference。
- Authentication / BCrypt / Filter / Interceptor / Spring Security / JWT 任務：只讀 Security reference。
- 若任務同時涉及多個領域，只讀必要的 2～3 個 reference，不要無差別全掃。
- 若 reference 與 DinoGo 正式需求、資料庫或現有 codebase 衝突，以 DinoGo 正式規範為優先。
- 找不到本機 reference 時，不得自行假設存在；直接依 `docs/teacher-code-style.md` 與現有 codebase 工作。

### 6.2 Teacher Code Style Extraction

第一次導入時，Codex 應從上述 reference 抽取可重複使用的 coding conventions，建立：

```text
docs/teacher-code-style.md
```

若內容較多，可再拆成：

```text
docs/teacher-style/
├── java-hibernate.md
├── spring-rest.md
├── vue.md
└── security.md
```

`docs/teacher-code-style.md` 作為總索引與共通原則。

之後一般任務先讀整理後的 style 文件；只有 style 文件不足以判斷時，再回頭搜尋對應 teacher reference。這可避免反覆讀取大量老師專案，降低不必要的 context 消耗。

### 6.3 Codex 對規範文件的讀取規則

本 `AGENTS.md` 是 Codex 的專案入口規範。Codex 在此檔案作用範圍內工作時，應自動遵守本檔案；本檔案再明確要求它依任務閱讀 `docs/` 中相應的正式文件。

重要區分：

- `AGENTS.md`：Codex 的自動專案指示入口。
- `docs/database-schema.md`：資料庫 Source of Truth。
- `docs/team-plan.md`：功能分工、每日進度、時程與驗收來源。
- `docs/git-workflow.md`：Git 分支與協作流程來源。
- `docs/teacher-code-style.md`：老師 coding style 的整理版。
- `teacher-reference/`：必要時才讀取的原始參考專案。

因此不要假設 Codex 會在每一個任務中自行完整讀遍所有 `docs/*.md`。本檔案要求：

1. 每次任務先判斷任務類型。
2. 主動讀取與任務相關的正式文件。
3. Git 操作前讀 `docs/git-workflow.md`。
4. DB / Entity / JPA / SQL 任務前讀 `docs/database-schema.md`。
5. 判斷模組 ownership、今日工作內容或進度時讀 `docs/team-plan.md`。
6. 實作程式碼風格時先讀 `docs/teacher-code-style.md`，必要時再讀對應 teacher reference。

### 6.4 每日進度的使用方式

`docs/team-plan.md` 是規範與計畫來源，但 Codex 不得把「計畫上的今天應完成什麼」誤認成使用者已經完成、正在做、或一定要立即執行的工作。

當使用者詢問「今天要做什麼」、「目前進度」、「下一步」或要求依每日規劃工作時：

1. 讀取 `docs/team-plan.md`。
2. 對照目前日期與模組。
3. 再檢查 Git working tree / 現有程式碼實際完成狀態。
4. 將「計畫」與「實際完成狀態」分開回報。
5. 不得只因 team plan 寫了某項任務，就假設該功能已完成。

若使用者直接指定今天的工作，使用者當前指示優先於原每日計畫。


## 7. Code Style 優先順序

實作時依照以下順序：

1. 使用者當前明確需求
2. `docs/database-schema.md`
3. `docs/team-plan.md`
4. `docs/git-workflow.md`
5. DinoGo 現有 codebase consistency
6. 本 `AGENTS.md`
7. `docs/teacher-code-style.md`
8. `teacher-reference/` 相關範例
9. 一般 Java / Spring Boot / Vue 慣例

若老師 style 與 DinoGo 現有架構衝突，優先維持 DinoGo 現有 codebase consistency，除非使用者明確要求改成老師風格。

---

## 8. Backend 開發規範

預設分層：

```text
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

原則：
- Controller：HTTP request / response
- Service：business logic
- Repository：persistence
- Entity：database mapping
- DTO：API request / response

避免 Controller 直接操作 Repository。

除非現有 DinoGo 架構或老師範例已明確採用其他一致模式，且使用者要求沿用。

---

## 9. Security 規範

- Password 使用 BCrypt
- Authentication 使用 JWT
- API 不得回傳 `password_hash`
- 不得儲存 plaintext password
- 不得在 console / log 印出完整 password
- 不得在 console / log 印出完整 JWT
- 不得把 JWT Secret、API Key、DB password commit 到 Git

---

## 10. JPA 規範

Entity mapping 必須以 `docs/database-schema.md` 為準。

重要欄位優先明確指定：

```java
@Table(name = "member")
@Column(name = "member_id")
@JoinColumn(name = "member_id")
```

不要因為 Hibernate naming strategy 可以自動推測，就在核心資料表完全依賴隱式 mapping。

關聯 mapping 必須符合正式 PK / FK。

---

## 11. Frontend 規範

預設：
- Vue 3
- Composition API
- `<script setup>`
- Vite
- Bootstrap 5
- Axios

除非有明確需求：
- 不改 Options API
- 不新增其他大型 UI framework
- 不大規模重構 UI
- 不修改其他組員頁面
- 不新增不必要 dependency

共用 UI 才抽成 components；頁面級元件放 views。

### 11.1 Router 規範（依 Vue teacher reference 整理）

Router 統一放在 `frontend/src/router/index.js`，建議維持以下結構：

1. 先集中 import `vue-router`、Layout 與必要的 route 元件。
2. 先宣告獨立的 `routes` 陣列，再呼叫 `createRouter` 建立 router，最後 `export default router`。
3. 每一筆 route 優先設定清楚的 `path`、具名 `name` 與 `component`。
4. 有父子頁面關係時使用 `children` 巢狀路由；父 Layout 負責放置 `RouterView`。
5. 頁面或程式導頁優先使用具名 route，例如 `router.push({ name: 'MemberProfile' })` 與 `<RouterLink :to="{ name: 'MemberProfile' }">`，避免散落硬編碼 URL。
6. 404 catch-all route 使用 `/:pathMatch(.*)*`，並放在 routes 陣列最後。
7. Layout、共用 shell 與 route guard 保留在 Router／Layout 層；頁面內容留在 `views`，不要在 view 重複建立 Header、Footer 或導覽 shell。
8. Teacher reference 的範例使用靜態 import；DinoGo 的頁面 route 預設採 dynamic import，以保留 Vite code splitting。只有 Layout 或確實需要立即載入的共用元件才使用靜態 import。

新增或調整 route 前，先確認路徑是否符合既有 `/member/*`、`/seller/*` 等 Layout 邊界，並檢查是否需要登入權限、404 fallback 與對應的 route name。`docs/design/router-branch-plan.md` 與 `docs/design/page-implementation-rules.md` 若有更具體規範，優先遵守 DinoGo 文件。

---

## 12. 最小修改原則

Codex 應只修改完成目前任務所需的最小範圍。

除非使用者明確要求，禁止：
- 大規模 refactor
- 全域 formatting
- 重命名 package
- 更換 framework
- 新增大型 dependency
- 修改其他模組 business logic
- 修改 API contract
- 修改 DB schema
- 刪除看似未使用但可能被其他組員依賴的程式碼

若發現其他問題，可列入 Notes，但不要順手修改。

---

## 13. 跨模組依賴規則

如果任務會影響其他模組，修改前先確認：

1. 依賴哪個模組
2. 使用哪些 API / DTO / Entity
3. 是否改變 API request / response
4. 是否改變 database schema
5. 是否需要其他組員同步修改
6. 是否可能造成 breaking change

API 有以下變更時，必須明確標記：
- URL
- HTTP Method
- Request 欄位
- Response 欄位
- 資料型態
- 欄位名稱
- validation
- authorization / role

---

## 14. Git 分支規則

正式分支架構：

```text
main
└── develop
    └── feature/*
```

### main
- 只放可展示、可交付、相對穩定版本
- 不直接開發
- 不直接 push 未測試內容

### develop
- 團隊整合分支
- 原則上不直接開發

### feature/*
- 個人功能開發分支

範例：

```text
feature/member-auth
feature/product-search
feature/cart-checkout
feature/order-payment
feature/seller-center
feature/chat-service
```

---

## 15. Codex Git Safety

團隊規範要求每日 Commit / Push，但這不代表 Codex 可自行對共享 repository 做遠端操作。

### 可直接使用於分析

```bash
git status
git branch
git diff
git log
git fetch origin
```

### 需要使用者當次明確要求才執行

```bash
git commit
git push
git merge
git rebase
git branch -d
git push origin --delete ...
建立 Pull Request
合併 Pull Request
```

### 禁止自行執行

```bash
git push --force
git reset --hard
git clean -fd
git init
rm -rf .git
```

不得刪除使用者或其他組員尚未 commit 的修改。

發生 Git 衝突時，先 `git status`，分析衝突內容，不要自行 Accept All。

---

## 16. 每日 Git Workflow

人工開發流程依 `docs/git-workflow.md`：

### 開始前

```bash
git status
git branch
git fetch origin
git switch develop
git pull origin develop
git switch feature/自己的分支
git merge develop
```

如果 feature branch 有未提交內容，不應直接 pull / switch；先 commit 或 stash。

### 開發中

小功能完成後 Commit。

Commit Message：

```text
<type>: <簡短描述>
```

常見 type：

```text
feat
fix
refactor
docs
style
test
chore
wip
```

### 每日結束

- 程式可啟動
- 完成基本測試
- Commit
- Push 自己的 feature branch
- GitHub 確認更新
- Discord / 群組回報進度

Codex 只負責協助檢查與準備；實際 commit / push 依前述 Git Safety 規則。

---

## 17. Pull Request Workflow

正式流程：

```text
feature/*
↓
Pull Request
↓
develop
↓
整合測試
↓
main
```

PR 前應確認：
- 程式可啟動
- 功能已測試
- 沒有敏感資料
- 沒有 `.env`
- 沒有 `application-local.properties`
- 沒有 `node_modules`
- 沒有 `target`
- API 變更已通知相關組員
- 沒有大量無關格式修改

---

## 18. 禁止提交的內容

```text
node_modules/
dist/
target/
.env
.env.*
application-local.properties
application-local.yml
*.log
.idea/
```

`.vscode/` 是否共享由團隊決定。

禁止提交：
- DB password
- JWT Secret
- API Key
- Email password
- Access Token
- 真實會員個資
- 教室或個人環境密碼
- teacher-reference 原始專案

---

## 19. Database Migration 規範

資料庫結構不得只在個人 SQL Server 手動修改。

建議：

```text
database/
├── migrations/
├── seed/
└── README.md
```

Migration 範例：

```text
V001__create_member_tables.sql
V002__create_product_tables.sql
V003__add_product_status.sql
```

若使用者批准 schema 變更，必須：

1. 更新 migration SQL
2. 更新 `docs/database-schema.md`
3. 說明影響的 Entity / Repository / Service / API
4. 通知受影響模組

禁止只改本機 DB 不留 SQL 紀錄。

---

## 20. 修改前的工作方式

小型、局部、低風險修改可以直接執行。

若涉及以下項目，先分析再修改：
- 多個檔案
- Authentication / JWT
- Spring Security
- Database mapping
- 共用 architecture
- API contract
- 跨模組 dependency
- 大規模 refactor

分析內容至少包含：

1. 理解的需求
2. 涉及哪些現有檔案
3. 預計修改哪些檔案
4. 每個檔案的改動
5. API 影響
6. DB 影響
7. 其他模組影響
8. breaking change 風險

如果使用者已明確要求直接執行，則不需重複等待確認，但仍應遵守最小修改原則。

---

## 21. 測試與驗證

### Backend

修改後若環境允許：

```bash
mvn test
```

必要時：

```bash
mvn spring-boot:run
```

至少確認：
- compile
- 相關 tests
- Spring Boot 可啟動（若此次修改可能影響啟動）

### Frontend

至少確認：

```bash
npm run build
```

必要時執行既有 lint / tests。

不要為了讓 test 變綠而任意修改其他模組。

---

## 22. 修改完成後回報格式

每次完成任務後，使用以下格式：

### Modified
- 列出修改檔案

### What changed
- 說明實際改動

### Validation
- compile
- test
- build
- 未執行的驗證及原因

### Cross-module impact
- 是否影響其他模組
- 是否有 API / DB contract 變化

### Risks / Notes
- 尚未處理問題
- 需要其他組員配合事項

不要只回答「完成」。

---

## 23. 第一次導入 Codex 時的任務

第一次使用本規範時，先不要修改 DinoGo business logic。

請依序：

1. 確認 DinoGo repository 位置
2. 確認目前 Git branch
3. 確認 `docs/database-schema.md`
4. 確認 `docs/team-plan.md`
5. 確認 `docs/git-workflow.md`
6. 檢查上述文件是否與本 AGENTS.md 有衝突
7. 若存在 `AGENTS.local.md`，讀取其中的本機 teacher-reference 路徑
8. 若有 teacher-reference，確認其為 READ ONLY
9. 分析老師與目前任務相關的 coding style
10. 建立 `docs/teacher-code-style.md`
11. 檢查 `.gitignore`
12. 若有 teacher-reference，確認其沒有被 Git 追蹤
13. 不修改 business logic
14. 不 commit
15. 不 push
16. 不 merge
17. 不建立 PR

完成後回報：

- 找到哪些規範文件
- 老師主要 coding patterns
- DinoGo 與老師 style 的差異
- 正式文件間的衝突
- teacher-reference 是否被 Git 追蹤
- 建議下一步

---

## 24. 全組共用與個人資訊邊界

本檔案是全組共用規範，因此：

- 不假設目前操作者是哪一位組員。
- 不寫死任何人的 feature branch。
- 不寫死任何人的每日實際進度。
- 不記錄個人本機路徑。
- 不記錄個人未公開 reference 檔案。
- 任務開始前應從當前 prompt、Git branch、`docs/team-plan.md` 與 `AGENTS.local.md` 判斷目前操作者工作範圍。
- 若無法確認操作者模組，只做不依賴個人 ownership 的分析；涉及跨模組修改時先指出影響。

## 25. 核心原則摘要

Codex 必須遵守以下原則：

1. Database Schema 不自行改。
2. 資料表 ownership 以 `database-schema.md` 為準。
3. 功能與每日時程以 `team-plan.md` 為準。
4. Git 流程以 `git-workflow.md` 為準。
5. teacher-reference 只讀，不進 Git。
6. 老師程式碼用來學 pattern，不直接複製 business logic。
7. 優先保持 DinoGo 現有 codebase consistency。
8. 跨模組修改前先判斷影響。
9. 只做完成任務所需的最小修改。
10. 不自行 push / merge / rebase / PR。
11. 修改後必須盡可能 compile / test / build。
12. 完成後清楚回報修改檔案、驗證結果與跨模組影響。
