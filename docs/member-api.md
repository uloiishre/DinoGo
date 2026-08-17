# 會員與帳號 API

本文件以目前後端實作為準，供前端與其他模組串接使用。

## 共通規則

- Base URL：`/api`
- Request body 使用 `Content-Type: application/json`。
- `/auth/**` 為公開 API；其餘本文件中的 API 都需要登入。
- 受保護 API 必須帶入：`Authorization: Bearer <token>`。
- `memberId` 一律從 JWT 取得，request body 與 URL 不接受前端指定的會員 ID。
- JWT 另有 `roles` claim，內容為 `buyer`、`seller`、`admin` 等小寫角色名稱陳列。
- Token 預設有效期為 1 小時，可由後端 `jwt.expiration-ms` 設定覆寫。

## 共用回應格式

### 會員資料 `MemberResponse`

```json
{
  "memberId": 1,
  "email": "member@example.com",
  "lastName": "王",
  "firstName": "小明",
  "birthDate": "2000-01-01",
  "phone": "0912345678",
  "status": "ACTIVE",
  "createdAt": "2026-08-17T10:00:00",
  "updatedAt": "2026-08-17T10:00:00"
}
```

`birthDate`、`phone`、`createdAt`、`updatedAt` 可能為 `null`；日期使用 ISO-8601 格式。

### 地址資料 `AddressResponse`

```json
{
  "addressId": 1,
  "receiverName": "王小明",
  "receiverPhone": "0912345678",
  "postalCode": "100",
  "city": "台北市",
  "district": "中正區",
  "detailAddress": "忠孝西路一段 1 號",
  "isDefault": true
}
```

### A 模組業務與驗證錯誤

註冊、登入、會員資料與地址 API 的 Controller 回傳下列 JSON 格式：

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "輸入資料驗證失敗",
  "fieldErrors": {
    "email": "must be a well-formed email address"
  }
}
```

- `fieldErrors` 只在 request 欄位驗證失敗時帶入欄位訊息；其他情況為空物件 `{}`。
- JSON 無法解析時，`message` 為 `請求內容格式錯誤`。
- URL 中的 `addressId` 小於等於 0 時，`message` 為 `請求參數驗證失敗`。
- 缺少、格式錯誤或過期的 JWT 由 Security 層處理，目前可能回傳純文字 401；前端應以 HTTP status 判斷登入失效，不應假設它是上述 JSON 格式。

## 認證 API

### 註冊

`POST /api/auth/register`

Request：

```json
{
  "email": "member@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "lastName": "王",
  "firstName": "小明",
  "birthDate": "2000-01-01",
  "phone": "0912345678"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `email` | 是 | Email 格式，最長 100 字 |
| `password` | 是 | 8～72 字 |
| `confirmPassword` | 是 | 8～72 字，且必須與 `password` 相同 |
| `lastName` | 是 | 最長 50 字 |
| `firstName` | 是 | 最長 50 字 |
| `birthDate` | 否 | `YYYY-MM-DD` |
| `phone` | 否 | 最長 20 字 |

成功：`200 OK`

```json
{
  "member": {
    "memberId": 1,
    "email": "member@example.com",
    "lastName": "王",
    "firstName": "小明",
    "birthDate": "2000-01-01",
    "phone": "0912345678",
    "status": "ACTIVE",
    "createdAt": "2026-08-17T10:00:00",
    "updatedAt": "2026-08-17T10:00:00"
  }
}
```

失敗：

| Status | `message` | 時機 |
| --- | --- | --- |
| 400 | `輸入資料驗證失敗` | 欄位不符合格式或長度 |
| 400 | `密碼與確認密碼不一致` | 兩個密碼不同 |
| 400 | `請求內容格式錯誤` | JSON 格式錯誤 |
| 409 | `Email 已被註冊` | Email 已存在 |

### 登入

`POST /api/auth/login`

Request：

```json
{
  "email": "member@example.com",
  "password": "password123"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `email` | 是 | Email 格式，最長 100 字 |
| `password` | 是 | 不可空白 |

成功：`200 OK`

```json
{
  "token": "<jwt>",
  "roles": ["buyer"],
  "member": {
    "memberId": 1,
    "email": "member@example.com",
    "lastName": "王",
    "firstName": "小明",
    "birthDate": "2000-01-01",
    "phone": "0912345678",
    "status": "ACTIVE",
    "createdAt": "2026-08-17T10:00:00",
    "updatedAt": "2026-08-17T10:00:00"
  }
}
```

失敗：

| Status | `message` | 時機 |
| --- | --- | --- |
| 400 | `輸入資料驗證失敗` | Request 欄位不合法 |
| 400 | `請求內容格式錯誤` | JSON 格式錯誤 |
| 401 | `Email 或密碼錯誤` | Email 不存在、密碼錯誤或帳號不是 `ACTIVE` |

登入成功後，將 `token` 儲存並加到後續請求的 `Authorization` header；`roles` 可供前端導覽與路由判斷使用，但後端授權仍只信任 JWT claim。不要把密碼或完整 token 寫入 log。

## 會員資料 API

以下 API 都需要 `Authorization: Bearer <token>`。

### 取得目前會員資料

`GET /api/member/profile`

成功：`200 OK`，body 為 `MemberResponse`。

失敗：

| Status | `message` | 時機 |
| --- | --- | --- |
| 401 | 依 Security 層回應 | 未登入或 token 無效 |
| 404 | `Member not found` | JWT 對應會員不存在 |

### 修改目前會員資料

`PUT /api/member/profile`

Request：

```json
{
  "lastName": "王",
  "firstName": "小明",
  "birthDate": "2000-01-01",
  "phone": "0912345678"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `lastName` | 是 | 最長 50 字 |
| `firstName` | 是 | 最長 50 字 |
| `birthDate` | 否 | `YYYY-MM-DD` |
| `phone` | 否 | 最長 20 字 |

成功：`200 OK`，body 為更新後的 `MemberResponse`。

失敗：400 欄位驗證、401 未登入或 token 無效、404 JWT 對應會員不存在。

## 收件地址 API

以下 API 都需要 `Authorization: Bearer <token>`。

地址建立與修改共用下列 Request：

```json
{
  "receiverName": "王小明",
  "receiverPhone": "0912345678",
  "postalCode": "100",
  "city": "台北市",
  "district": "中正區",
  "detailAddress": "忠孝西路一段 1 號",
  "isDefault": true
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `receiverName` | 是 | 最長 100 字 |
| `receiverPhone` | 是 | 最長 20 字 |
| `postalCode` | 否 | 最長 10 字 |
| `city` | 是 | 最長 50 字 |
| `district` | 是 | 最長 50 字 |
| `detailAddress` | 是 | 最長 255 字 |
| `isDefault` | 否 | `true`、`false` 或省略 |

規則：新增第一筆地址時會自動成為預設地址；更新時省略 `isDefault` 代表維持原設定。

### 取得目前會員的地址清單

`GET /api/addresses`

成功：`200 OK`，回傳 `AddressResponse` 陣列，預設地址排在第一筆。

### 取得單筆地址

`GET /api/addresses/{addressId}`

成功：`200 OK`，body 為 `AddressResponse`。

### 新增地址

`POST /api/addresses`

body 使用上述地址 Request。

成功：`201 Created`，body 為建立後的 `AddressResponse`。

### 修改地址

`PUT /api/addresses/{addressId}`

body 使用上述地址 Request。

成功：`200 OK`，body 為更新後的 `AddressResponse`。

### 刪除地址

`DELETE /api/addresses/{addressId}`

成功：`204 No Content`，沒有 response body。

地址 API 的共同失敗情況：

| Status | `message` | 時機 |
| --- | --- | --- |
| 400 | `輸入資料驗證失敗` | 建立或修改的 body 欄位不合法 |
| 400 | `請求內容格式錯誤` | JSON 格式錯誤 |
| 400 | `請求參數驗證失敗` | `addressId` 小於等於 0 |
| 401 | 依 Security 層回應 | 未登入或 token 無效 |
| 404 | `Address not found` | 地址不存在或不屬於目前登入會員 |
| 409 | `此地址已被訂單使用，無法刪除` | 刪除的地址已被訂單引用 |

## 跨模組串接注意事項

- C（購物車／結帳）可用 `GET /api/addresses` 取得登入會員地址；建立訂單時仍應由 D 的訂單 API 定義其 request 欄位，勿直接使用 A 的 Entity。
- E（賣家）與 F（聊天室／客服）需要目前登入會員時，應由 JWT 的 `memberId` 取得身分，不接受前端自行指定其他會員 ID。
- JWT 同時提供 `memberId` 與 `roles` claim。各模組可依 `roles` 顯示導覽項目，但後端必須依 Spring Security authorities 執行實際授權。
