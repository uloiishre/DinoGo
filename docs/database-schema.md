# Database Schema

> 模組責任分配
> A：會員與帳號模組
> B：商品目錄模組
> C：購物車與收藏模組
> D：訂單、付款與物流模組
> E：賣家中心模組（優惠券與 AI 銷售分析）
> F：通知、評價與客服模組
> AI：對話對照模組（全體組員暫定負責）

## 文件使用說明

本文件主要有三個程式碼區塊，請依用途使用：

| 區塊      | 中文標題                 | 用途                                                                                                       | 是否可直接在 MSSQL 執行                                       |
| --------- | ------------------------ | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| DBML      | ER Diagram（DBML）       | 給 dbdiagram.io 產生 ERD 圖，可貼到 <https://dbdiagram.io/d>。                                             | 不可，DBML 是畫 ERD 的描述語法，不是 T-SQL                    |
| Seed Data | Seed Data（範例資料）    | 建表完成後使用的開發測試資料。                                                                             | 可以，但要先建立資料表                                        |
| T-SQL DDL | MSSQL 建表腳本（完整版） | MSSQL 正式建表腳本，包含 `CREATE TABLE`、`CREATE SCHEMA`、`CREATE INDEX`、PK、FK、UNIQUE、CHECK、DEFAULT。 | 可以，建議貼到 SSMS / Azure Data Studio，先選好空資料庫再執行 |

注意：DBML 是 ERD 繪圖用語法；MSSQL 建表請使用後面的「MSSQL 建表腳本（完整版）」區塊。

## 0811 實際資料庫匯出比對結果

本文件的 35 張表是原先的設計規格；依 2026/08/11 的 SSMS 物件總管確認，
目前 `DinoGo` 實際有 29 張資料表與 10 個 Schema。`DinoGo_0811_schema.sql`
也與此實際狀態一致。

| 項目 | 文件設計 | 0811 實際匯出 |
| ---- | -------- | ------------ |
| 資料表數量 | 35 張 | 29 張 |
| Schema 數量 | 9 個 | 10 個 |
| 實際多出的 Schema | 無 | `sysmsg`（目前沒有資料表） |
| 文件中尚未建立的表 | 無 | `review.ProductRecord`、`review.MemberRecord`、`msg.MsgTemplate`、`msg.MsgSample`、`msg.Msg`、`msg.MsgRecipient` |

因此目前資料庫實際狀態與文件設計規格不一致。MSSQL DDL 區塊仍保留 35 張表的設計版本；若 6 張表已確定不再使用，才應進一步同步刪除 DBML、DDL、責任分工與驗收標準中的相關內容。

## ER Diagram（DBML）

以下是給 dbdiagram.io 使用的 ER Diagram 程式碼，不是 MSSQL 建表語法。

```dbml
// 區塊 1：ER Diagram（DBML）
// 用途：貼到 dbdiagram.io 產生 ERD 圖，不可直接在 MSSQL 執行。
Project database_schema {
  database_type: 'SQL Server'
}

// A：會員與帳號模組
Table member.Member {
  member_id int [pk, increment]
  email varchar(100) [not null, unique]
  password_hash varchar(255) [not null]
  last_name nvarchar(50) [not null]
  first_name nvarchar(50) [not null]
  birth_date date
  phone varchar(20)
  status varchar(20) [not null, default: 'ACTIVE']
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]
}

// A：會員與帳號模組
Table member.Address {
  address_id int [pk, increment]
  member_id int [not null, ref: > member.Member.member_id]
  receiver_name nvarchar(100) [not null]
  receiver_phone varchar(20) [not null]
  postal_code varchar(10)
  city nvarchar(50) [not null]
  district nvarchar(50) [not null]
  detail_address nvarchar(255) [not null]
  is_default bit [not null, default: 0]
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]
}

// A：會員與帳號模組
Table member.Role {
  role_id int [pk, increment]
  role_name varchar(50) [not null, unique]
  description nvarchar(100)
}

// A：會員與帳號模組
Table member.MemberRole {
  member_id int [not null, ref: > member.Member.member_id]
  role_id int [not null, ref: > member.Role.role_id]
  assigned_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (member_id, role_id) [pk]
  }
}

// A：會員與帳號模組
Table member.MemberOAuthAccount {
  oauth_account_id int [pk, increment]
  member_id int [not null, ref: > member.Member.member_id]
  provider varchar(30) [not null]
  provider_user_id varchar(255) [not null]
  provider_email varchar(255)
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (provider, provider_user_id) [unique]
    (member_id, provider) [unique]
  }
}

// E：賣家中心模組 - 商家資料
Table seller.Seller {
  seller_id int [pk, increment]
  member_id int [not null, unique, ref: - member.Member.member_id]
  store_name varchar(100) [not null]
  store_description varchar(500)
  store_logo_url varchar(255)
  status varchar(30) [not null]
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]
}

// B：商品目錄模組
Table catalog.Category {
  category_id int [pk, increment]
  category_name nvarchar(100) [not null, unique]
}

// B：商品目錄模組
Table catalog.Subcategory {
  subcategory_id int [pk, increment]
  category_id int [not null, ref: > catalog.Category.category_id]
  subcategory_name nvarchar(100) [not null]

  indexes {
    (category_id, subcategory_name) [unique]
  }
}

// B：商品目錄模組
Table catalog.Brand {
  brand_id int [pk, increment]
  brand_name nvarchar(100) [not null]
}

// B：商品目錄模組
Table catalog.Product {
  product_id int [pk, increment]
  seller_id int [not null, ref: > seller.Seller.seller_id]
  subcategory_id int [not null, ref: > catalog.Subcategory.subcategory_id]
  brand_id int [not null, ref: > catalog.Brand.brand_id]
  product_name nvarchar(50) [not null]
  description nvarchar(3000)
  base_price decimal(10,2) [not null]
  status bit [not null, default: 1]
  view_count int [not null, default: 0]
  sold_count int [not null, default: 0]
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]
}

// B：商品目錄模組
Table catalog.ProductSku {
  sku_id int [pk, increment]
  product_id int [not null, ref: > catalog.Product.product_id]
  spec1_name nvarchar(30)
  spec1_value nvarchar(50)
  spec2_name nvarchar(30)
  spec2_value nvarchar(50)
  price decimal(10,2) [not null]
  stock int [not null]
  status tinyint [not null, default: 1]
}

// B：商品目錄模組
Table catalog.ProductImage {
  image_id int [pk, increment]
  product_id int [not null, ref: > catalog.Product.product_id]
  image_url nvarchar(255) [not null]
  sort_order int [not null]
  is_main bit [not null, default: 0]
}

// C：購物車與收藏模組
Table cart.Cart {
  cart_id int [pk, increment]
  member_id int [not null, unique, ref: - member.Member.member_id]
}

// C：購物車與收藏模組
Table cart.CartItem {
  cart_item_id int [pk, increment]
  cart_id int [not null, ref: > cart.Cart.cart_id]
  sku_id int [not null, ref: > catalog.ProductSku.sku_id]
  quantity int [not null]

  indexes {
    (cart_id, sku_id) [unique]
  }
}

// C：購物車與收藏模組
Table cart.Favorite {
  favorite_id int [pk, increment]
  member_id int [not null, ref: > member.Member.member_id]
  product_id int [not null, ref: > catalog.Product.product_id]

  indexes {
    (member_id, product_id) [unique]
  }
}

// ======================================================
// D：訂單、付款與物流模組
// 用途：貼至 dbdiagram.io 產生 ER Diagram
//
// 外部關聯資料表：
// member.Member
// seller.Seller
// member.Address
// catalog.Product
// catalog.ProductSku
// ======================================================


// ------------------------------------------------------
// 訂單主表
// ------------------------------------------------------
Table sales.Orders {
  order_id int [pk, increment]

  order_no varchar(30) [
    not null,
    unique,
    note: '訂單編號，由後端產生，例如 ORD202608020001'
  ]

  buyer_id int [
    not null,
    ref: > member.Member.member_id,
    note: '下單會員 ID'
  ]

  seller_id int [
    not null,
    ref: > seller.Seller.seller_id,
    note: '此訂單所屬賣家；MVP 一張訂單只屬於一個賣家'
  ]

  address_id int [
    ref: > member.Address.address_id,
    note: '下單時選擇的地址 ID；歷史訂單顯示應以地址快照欄位為準'
  ]

  // 收件資料快照
  receiver_name nvarchar(100) [
    not null,
    note: '下單當下的收件人姓名快照'
  ]

  receiver_phone varchar(20) [
    not null,
    note: '下單當下的收件人電話快照'
  ]

  shipping_postal_code varchar(10) [
    note: '下單當下的郵遞區號快照'
  ]

  shipping_city nvarchar(50) [
    not null,
    note: '下單當下的縣市快照'
  ]

  shipping_district nvarchar(50) [
    not null,
    note: '下單當下的行政區快照'
  ]

  shipping_detail_address nvarchar(255) [
    not null,
    note: '下單當下的詳細地址快照'
  ]

  status varchar(30) [
    not null,
    default: 'PENDING_PAYMENT',
    note: '合法值：PENDING_PAYMENT、PAID、PROCESSING、SHIPPED、COMPLETED、CANCELLED'
  ]

  subtotal_amount decimal(12,2) [
    not null,
    note: '商品小計，由後端重新計算；必須 >= 0'
  ]

  shipping_fee decimal(12,2) [
    not null,
    default: 0,
    note: '運費；必須 >= 0'
  ]

  discount_amount decimal(12,2) [
    not null,
    default: 0,
    note: '折扣金額；必須 >= 0'
  ]

  total_amount decimal(12,2) [
    not null,
    note: '訂單總額，由後端計算；必須 >= 0'
  ]

  buyer_remark nvarchar(500) [
    note: '買家訂單備註'
  ]

  cancel_reason nvarchar(500) [
    note: '取消原因'
  ]

  cancelled_by varchar(20) [
    note: '取消者類型，建議合法值：BUYER、SELLER、SYSTEM'
  ]

  cancelled_at datetime2 [
    note: '訂單取消時間'
  ]

  completed_at datetime2 [
    note: '訂單完成時間'
  ]

  created_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  updated_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  indexes {
    buyer_id [name: 'ix_orders_buyer_id']
    seller_id [name: 'ix_orders_seller_id']
    status [name: 'ix_orders_status']

    (buyer_id, created_at) [
      name: 'ix_orders_buyer_created_at'
    ]

    (seller_id, status, created_at) [
      name: 'ix_orders_seller_status_created_at'
    ]
  }

  Note: '''
  訂單狀態合法值：
  PENDING_PAYMENT、PAID、PROCESSING、SHIPPED、COMPLETED、CANCELLED。

  狀態轉換由後端 Service 控制：
  PENDING_PAYMENT → PAID
  PENDING_PAYMENT → CANCELLED
  PAID → PROCESSING
  PROCESSING → SHIPPED
  SHIPPED → COMPLETED

  正式 MSSQL DDL 需建立狀態及金額 CHECK CONSTRAINT。
  '''
}


// ------------------------------------------------------
// 訂單商品明細
// ------------------------------------------------------
Table sales.OrderItem {
  order_item_id int [pk, increment]

  order_id int [
    not null,
    ref: > sales.Orders.order_id,
    note: '所屬訂單'
  ]

  product_id int [
    not null,
    ref: > catalog.Product.product_id,
    note: '原始商品 ID；商品顯示仍應使用快照'
  ]

  sku_id int [
    not null,
    ref: > catalog.ProductSku.sku_id,
    note: '原始 SKU ID；SKU 顯示仍應使用快照'
  ]

  // 商品資料快照
  product_name nvarchar(100) [
    not null,
    note: '下單當下的商品名稱快照'
  ]

  sku_spec nvarchar(200) [
    note: '下單當下的 SKU 規格快照，例如：黑色 / XL'
  ]

  product_image_url nvarchar(500) [
    note: '下單當下的商品主圖網址快照'
  ]

  unit_price decimal(12,2) [
    not null,
    note: '下單當下的商品單價；由後端取得 SKU 價格；必須 >= 0'
  ]

  quantity int [
    not null,
    note: '購買數量；必須 > 0'
  ]

  subtotal decimal(12,2) [
    not null,
    note: '商品明細小計，unit_price × quantity；必須 >= 0'
  ]

  is_reviewed bit [
    not null,
    default: 0,
    note: '是否已建立商品評價；實際評價內容存於 F 模組'
  ]

  created_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  indexes {
    order_id [name: 'ix_order_item_order_id']
    product_id [name: 'ix_order_item_product_id']
    sku_id [name: 'ix_order_item_sku_id']
    is_reviewed [name: 'ix_order_item_is_reviewed']
  }

  Note: '''
  product_name、sku_spec、product_image_url、unit_price 為下單快照。
  商品之後改名、改規格、改價格或下架，不應影響歷史訂單內容。

  正式 MSSQL DDL 需限制：
  unit_price >= 0
  quantity > 0
  subtotal >= 0
  '''
}


// ------------------------------------------------------
// 付款方式
// ------------------------------------------------------
Table sales.PaymentMethod {
  payment_method_id int [pk, increment]

  method_code varchar(30) [
    not null,
    unique,
    note: '付款方式代碼，例如 CREDIT_CARD、BANK_TRANSFER、CASH_ON_DELIVERY'
  ]

  method_name nvarchar(50) [
    not null,
    note: '付款方式顯示名稱，例如信用卡、銀行轉帳、貨到付款'
  ]

  created_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  updated_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  Note: '''
  payment_method 為共用付款方式主檔。
  method_code 必須保持 UNIQUE。
  MVP 若不需要啟用或停用付款方式，可不加入 is_active。
  '''
}


// ------------------------------------------------------
// 付款紀錄
// ------------------------------------------------------
Table sales.Payment {
  payment_id int [pk, increment]

  payment_no varchar(40) [
    not null,
    unique,
    note: '付款編號，由後端產生'
  ]

  order_id int [
    not null,
    ref: > sales.Orders.order_id,
    note: '付款所屬訂單；不可設定 UNIQUE，一張訂單可有多次付款嘗試'
  ]

  payment_method_id int [
    not null,
    ref: > sales.PaymentMethod.payment_method_id,
    note: '付款方式'
  ]

  amount decimal(12,2) [
    not null,
    note: '付款金額，由 orders.total_amount 帶入；必須 >= 0'
  ]

  status varchar(20) [
    not null,
    default: 'PENDING',
    note: '合法值：PENDING、SUCCESS、FAILED、CANCELLED'
  ]

  transaction_no varchar(100) [
    note: '第三方交易編號或模擬付款交易編號'
  ]

  failure_reason nvarchar(255) [
    note: '付款失敗原因'
  ]

  paid_at datetime2 [
    note: '付款成功時間'
  ]

  created_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  updated_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  indexes {
    order_id [name: 'ix_payment_order_id']
    payment_method_id [name: 'ix_payment_payment_method_id']
    status [name: 'ix_payment_status']

    (order_id, status) [
      name: 'ix_payment_order_status'
    ]
  }

  Note: '''
  payment.order_id 不可加入 UNIQUE。

  一張訂單可以有多筆付款紀錄，例如：
  第一次付款 FAILED
  第二次付款 SUCCESS

  同一張訂單最多只能有一筆 SUCCESS，
  此規則由後端 Service 驗證。

  正式 MSSQL DDL 需限制：
  amount >= 0
  status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')
  '''
}


// ------------------------------------------------------
// 物流資料
// ------------------------------------------------------
Table sales.Shipment {
  shipment_id int [pk, increment]

  order_id int [
    not null,
    unique,
    ref: - sales.Orders.order_id,
    note: 'MVP 一張訂單只建立一筆物流資料'
  ]

  carrier_name nvarchar(100) [
    note: '物流商名稱'
  ]

  tracking_no varchar(100) [
    note: '物流追蹤編號'
  ]

  status varchar(30) [
    not null,
    default: 'PREPARING',
    note: '合法值：PREPARING、SHIPPED、AVAILABLE_FOR_PICKUP、DELIVERED'
  ]

  shipped_at datetime2 [
    note: '賣家出貨時間'
  ]

  available_pickup_at datetime2 [
    note: '商品可取貨時間'
  ]

  delivered_at datetime2 [
    note: '商品送達或取貨完成時間'
  ]

  delivery_photo_url nvarchar(500) [
    note: '送達證明或配送照片網址'
  ]

  created_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  updated_at datetime2 [
    not null,
    default: `SYSDATETIME()`
  ]

  indexes {
    status [name: 'ix_shipment_status']
    tracking_no [name: 'ix_shipment_tracking_no']
  }

  Note: '''
  shipment.order_id 保持 UNIQUE，
  代表目前 MVP 為 orders 與 shipment 一對一。

  正式 MSSQL DDL 需限制：
  status IN (
    'PREPARING',
    'SHIPPED',
    'AVAILABLE_FOR_PICKUP',
    'DELIVERED'
  )
  '''
}

// F：評價模組
Table review.ProductRecord {
  product_record_id int [pk, increment]
  order_item_id int [not null, unique, ref: - sales.OrderItem.order_item_id]
  reviewer_id int [not null, ref: > member.Member.member_id]
  rating tinyint [not null, note: '1-5']
  content nvarchar(1000)
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    reviewer_id
  }
}

// F：評價模組
Table review.MemberRecord {
  member_record_id int [pk, increment]
  order_id int [not null, ref: > sales.Orders.order_id]
  reviewer_id int [not null, ref: > member.Member.member_id]
  target_member_id int [not null, ref: > member.Member.member_id]
  rating tinyint [not null, note: '1-5']
  content nvarchar(1000)
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (order_id, reviewer_id, target_member_id) [unique]
    order_id
    reviewer_id
    target_member_id
  }
}

// E：賣家中心模組 - 優惠券
Table seller.Coupon {
  coupon_id int [pk, increment]
  seller_id int [not null, ref: > seller.Seller.seller_id]
  coupon_code varchar(100) [not null]
  coupon_name varchar(100) [not null]
  discount_type varchar(30) [not null, note: 'PERCENT, AMOUNT']
  discount_value decimal(18,2) [not null, note: '> 0']
  min_purchase_amount decimal(18,2)
  start_at datetime2 [not null]
  end_at datetime2 [not null]
  limit_count int [note: 'NULL or > 0']
  used_count int [not null, default: 0]
  scope_type varchar(30) [not null, note: 'ALL, CATEGORY, PRODUCT']
  category_id int [ref: > catalog.Category.category_id]
  product_id int [ref: > catalog.Product.product_id]
  status varchar(30) [not null, note: 'DRAFT, ACTIVE, DISABLED, EXPIRED']
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (seller_id, coupon_code) [unique]
  }
}

// E：賣家中心模組 - 會員優惠券領取紀錄
// 透過 coupon.coupon_id 串接 coupon，再由 coupon.seller_id 對應 seller。
Table seller.MemberCoupon {
  member_coupon_id int [pk, increment]
  coupon_id int [not null, ref: > seller.Coupon.coupon_id]
  member_id int [not null, ref: > member.Member.member_id]
  is_used bit [not null, default: 0]
  used_at datetime2
  received_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (coupon_id, member_id) [unique]
  }
}

// E：賣家中心模組 - AI 銷售分析
Table seller.SellerAiSalesAnalysis {
  analysis_id int [pk, increment]
  seller_id int [not null, ref: > seller.Seller.seller_id]
  analysis_period_start date [not null]
  analysis_period_end date [not null]
  revenue_amount decimal(12,2) [not null, default: 0, note: '>= 0']
  order_count int [not null, default: 0, note: '>= 0']
  product_count int [not null, default: 0, note: '>= 0']
  used_coupon_count int [not null, default: 0, note: '>= 0']
  top_product_summary nvarchar(500)
  coupon_summary nvarchar(500)
  risk_summary nvarchar(500)
  ai_summary nvarchar(1000) [not null]
  ai_recommendation nvarchar(1000)
  model_name varchar(100)
  generated_at datetime2 [not null, default: `SYSDATETIME()`]
  created_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (seller_id, generated_at) [name: 'ix_seller_ai_sales_analysis_seller_generated']
    (seller_id, analysis_period_start, analysis_period_end) [name: 'ix_seller_ai_sales_analysis_period']
  }
}

// F：通知模組
Table msg.MsgTemplate {
  template_id int [pk, increment]
  role_type char(2) [not null]
  msg_type varchar(50) [not null]
  title nvarchar(50) [not null]
  content nvarchar(500) [not null]
  coupon_id int [ref: > seller.Coupon.coupon_id]
}

// F：通知模組
Table msg.MsgSample {
  sample_id int [pk, increment]
  role_type char(2) [not null]
  msg_type varchar(50) [not null]
  sample_subject nvarchar(500) [not null]
  sample_content nvarchar(500) [not null]
  sample_at datetime2 [not null, default: `SYSDATETIME()`]
}

// F：通知模組
Table msg.Msg {
  msg_id int [pk, increment]
  template_id int [ref: > msg.MsgTemplate.template_id]
  sender_id int [not null, ref: > member.Member.member_id]
  title nvarchar(200) [not null]
  content nvarchar(5000) [not null]
  created_at datetime2 [not null, default: `SYSDATETIME()`]
}

// F：通知模組
Table msg.MsgRecipient {
  msg_recipient_id int [pk, increment]
  msg_id int [not null, ref: > msg.Msg.msg_id]
  member_id int [not null, ref: > member.Member.member_id]
  is_read bit [not null, default: 0]
  read_at datetime2
  delivered_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (msg_id, member_id) [unique]
  }
}

// AI：對話對照模組
Table ai.AiConversation {
  conversation_id int [pk, increment]
  cloud_conversation_id varchar(255) [not null]
  member_id int [not null, ref: > member.Member.member_id]
  provider varchar(50) [not null]
  log_file_path nvarchar(500) [not null]
  created_at datetime2 [not null, default: `SYSDATETIME()`]
  updated_at datetime2 [not null, default: `SYSDATETIME()`]

  indexes {
    (provider, cloud_conversation_id) [unique]
  }
}

// F：客服模組：service schema
Table service.Role {
  service_role_id int [pk]
  role_name varchar(50) [not null, note: 'customer / seller']
}

Table service.Topic {
  topic_id char(1) [pk]
  role_name varchar(50) [not null]
  topic nvarchar(50)
  topic_enter nvarchar(100)
}

Table service.Subtheme {
  subtheme_id int [pk]
  role_name varchar(50) [not null]
  topic nvarchar(50)
  subtheme nvarchar(50)
  subtheme_enter nvarchar(100)
}

Table service.Demand {
  demand_id int [pk]
  role_name varchar(50) [not null]
  subtheme nvarchar(50)
  demand nvarchar(50)
  demand_enter nvarchar(100)
}

Table service.Reply {
  sys_reply_id int [pk]
  role_name varchar(50) [not null]
  demand nvarchar(50)
  reply nvarchar(100)
  reply_enter nvarchar(100)
}

```

## 模組責任對照表

| 組員             | 負責資料表                                                                                                                                                                      | 說明                                                                            |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| A                | `member.Member`, `member.Address`, `member.Role`, `member.MemberRole`, `member.MemberOAuthAccount`                                                                                                              | 會員與帳號模組，負責會員資料、地址、角色、權限關聯與第三方登入對照              |
| B                | `catalog.Category`, `catalog.Subcategory`, `catalog.Brand`, `catalog.Product`, `catalog.ProductSku`, `catalog.ProductImage`                                                                                                   | 商品目錄模組，負責商品分類、品牌、商品與商品 SKU 管理                           |
| C                | `cart.Cart`, `cart.CartItem`, `cart.Favorite`                                                                                                                                                 | 購物車與收藏模組，負責購物車內容與會員收藏清單                                  |
| D                | `sales.Orders`, `sales.OrderItem`, `sales.PaymentMethod`, `sales.Payment`, `sales.Shipment`                                                                                                                 | 訂單、付款與物流模組，負責訂單流轉、付款紀錄與物流狀態                          |
| E                | `seller.Seller`, `seller.Coupon`, `seller.MemberCoupon`, `seller.SellerAiSalesAnalysis`                                                                                                                  | 賣家中心後台模組，負責商家資料、賣家優惠券、會員領券紀錄與 AI 銷售分析           |
| F                | `msg.MsgTemplate`, `msg.MsgSample`, `msg.Msg`, `msg.MsgRecipient`, `review.ProductRecord`, `review.MemberRecord`, `service.Role`, `service.Topic`, `service.Subtheme`, `service.Demand`, `service.Reply` | 通知、評價與客服模組，負責正式訊息、收件狀態、商品/會員評價、客服分類與系統回覆 |
| 全體組員（暫定） | `ai.AiConversation`                                                                                                                                                               | AI 對話對照模組，暫定由全體組員共同負責                                         |

## 建表順序（MSSQL）

```text
01_create_member_tables.sql
02_create_account_seller_tables.sql
03_create_catalog_tables.sql
04_create_product_tables.sql
05_create_cart_tables.sql
06_create_order_tables.sql
07_create_seller_center_tables.sql
08_create_review_tables.sql
09_create_msg_tables.sql
10_create_ai_tables.sql
11_create_service_tables.sql
90_insert_seed_data.sql
```

### 建表順序說明

1. 先建立 `member.Member`、`member.Role`
2. 再建立 `member.Address`、`member.MemberRole`、`member.MemberOAuthAccount`、`seller.Seller`
3. 再建立 `catalog.Category`、`catalog.Subcategory`、`catalog.Brand`
4. 再建立 `catalog.Product`、`catalog.ProductSku`、`catalog.ProductImage`
5. 再建立 `cart.Cart`、`cart.CartItem`、`cart.Favorite`
6. 再建立 `sales.Orders`、`sales.OrderItem`、`sales.PaymentMethod`、`sales.Payment`、`sales.Shipment`
7. 再建立 `seller.Coupon`、`seller.MemberCoupon`、`seller.SellerAiSalesAnalysis`
8. 建立 `review.ProductRecord`、`review.MemberRecord`
9. 建立 `msg.MsgTemplate`、`msg.MsgSample`、`msg.Msg`、`msg.MsgRecipient`
10. 建立 `ai.AiConversation`
11. 最後建立 `service.Role`、`service.Topic`、`service.Subtheme`、`service.Demand`、`service.Reply`

> `seller.Seller` 歸 E 賣家中心後台負責，但因為 B 商品、D 訂單、E 優惠券與 AI 銷售分析都會參照 `seller.Seller.seller_id`，所以建表時需先在第 2 步建立。

## Seed Data（範例資料）

這一段是測試用範例資料，不是建表語法。請先執行「MSSQL 建表腳本（完整版）」建立資料表，再依外鍵順序執行 Seed Data。

> 責任對應：
>
> - A：會員、角色與身分資料
> - B：分類、品牌、商品與 SKU 資料
> - D：付款方式與訂單相關資料
> - E：賣家中心資料，包含商家資料、優惠券、會員領券紀錄與賣家 AI 銷售分析
> - F：通知範本、評價與客服資料
> - 全體組員：AI 對話資料需搭配實際功能再補

```sql
-- 區塊 2：Seed Data（範例資料）
-- 用途：建表完成後執行，用來建立開發測試資料。

-- A：角色資料
INSERT INTO member.Role (role_id, role_name, description)
VALUES
    (1, 'buyer', '一般會員'),
    (2, 'seller', '商家會員'),
    (3, 'admin', '管理員');

-- A：會員資料
SET IDENTITY_INSERT member.Member ON;
INSERT INTO member.Member (
    member_id,
    email,
    password_hash,
    last_name,
    first_name,
    birth_date,
    phone,
    status,
    created_at,
    updated_at
)
VALUES
    (1, 'buyer01@example.com', 'hash_buyer_01', '王', '小明', '1998-03-15', '0912345678', 'ACTIVE', SYSDATETIME(), SYSDATETIME()),
    (2, 'seller01@example.com', 'hash_seller_01', '李', '小華', '1990-07-22', '0923456789', 'ACTIVE', SYSDATETIME(), SYSDATETIME()),
    (3, 'admin01@example.com', 'hash_admin_01', '張', '管理', '1988-11-05', '0934567890', 'ACTIVE', SYSDATETIME(), SYSDATETIME());
SET IDENTITY_INSERT member.Member OFF;

-- A：會員角色資料
INSERT INTO member.MemberRole (member_id, role_id, assigned_at)
VALUES
    (1, 1, SYSDATETIME()),
    (2, 2, SYSDATETIME()),
    (3, 3, SYSDATETIME());

-- E：賣家中心資料 - 商家資料
SET IDENTITY_INSERT seller.Seller ON;
INSERT INTO seller.Seller (
    seller_id,
    member_id,
    store_name,
    store_description,
    store_logo_url,
    status,
    created_at,
    updated_at
)
VALUES
    (1, 2, '小華生活館', '生活用品與日常商品', 'https://example.com/logo1.png', 'ACTIVE', SYSDATETIME(), SYSDATETIME());
SET IDENTITY_INSERT seller.Seller OFF;

-- B：分類資料
SET IDENTITY_INSERT catalog.Category ON;
INSERT INTO catalog.Category (category_id, category_name)
VALUES
    (1, '家電'),
    (2, '服飾'),
    (3, '食品');
SET IDENTITY_INSERT catalog.Category OFF;

SET IDENTITY_INSERT catalog.Subcategory ON;
INSERT INTO catalog.Subcategory (subcategory_id, category_id, subcategory_name)
VALUES
    (1, 1, '電器'),
    (2, 2, '男裝'),
    (3, 3, '零食');
SET IDENTITY_INSERT catalog.Subcategory OFF;

SET IDENTITY_INSERT catalog.Brand ON;
INSERT INTO catalog.Brand (brand_id, brand_name)
VALUES
    (1, 'A品牌'),
    (2, 'B品牌'),
    (3, 'C品牌');
SET IDENTITY_INSERT catalog.Brand OFF;

-- B：商品資料
SET IDENTITY_INSERT catalog.Product ON;
INSERT INTO catalog.Product (
    product_id,
    seller_id,
    subcategory_id,
    brand_id,
    product_name,
    description,
    base_price,
    status,
    view_count,
    sold_count,
    created_at,
    updated_at
)
VALUES
    (1, 1, 1, 1, '吹風機', '高效能吹風機', 799.00, 1, 120, 25, SYSDATETIME(), SYSDATETIME()),
    (2, 1, 2, 2, '休閒T恤', '舒適透氣休閒T恤', 299.00, 1, 55, 10, SYSDATETIME(), SYSDATETIME());
SET IDENTITY_INSERT catalog.Product OFF;

-- B：SKU 資料
SET IDENTITY_INSERT catalog.ProductSku ON;
INSERT INTO catalog.ProductSku (
    sku_id,
    product_id,
    spec1_name,
    spec1_value,
    spec2_name,
    spec2_value,
    price,
    stock,
    status
)
VALUES
    (1, 1, '顏色', '白色', NULL, NULL, 799.00, 50, 1),
    (2, 2, '顏色', '黑色', '尺寸', 'M', 299.00, 30, 1);
SET IDENTITY_INSERT catalog.ProductSku OFF;

-- D：付款方式資料
SET IDENTITY_INSERT sales.PaymentMethod ON;
INSERT INTO sales.PaymentMethod (
    payment_method_id,
    method_code,
    method_name,
    created_at,
    updated_at
)
VALUES
    (1, 'CREDIT_CARD', '信用卡', SYSDATETIME(), SYSDATETIME()),
    (2, 'LINE_PAY', 'LINE Pay', SYSDATETIME(), SYSDATETIME()),
    (3, 'CASH_ON_DELIVERY', '貨到付款', SYSDATETIME(), SYSDATETIME());
SET IDENTITY_INSERT sales.PaymentMethod OFF;

-- E：賣家中心資料 - 優惠券資料
SET IDENTITY_INSERT seller.Coupon ON;
INSERT INTO seller.Coupon (
    coupon_id,
    seller_id,
    coupon_code,
    coupon_name,
    discount_type,
    discount_value,
    min_purchase_amount,
    start_at,
    end_at,
    limit_count,
    used_count,
    scope_type,
    category_id,
    product_id,
    status,
    created_at,
    updated_at
)
VALUES
    (1, 1, 'SAVE10', '新會員9折券', 'PERCENT', 10.00, 500.00, SYSDATETIME(), DATEADD(day, 30, SYSDATETIME()), 100, 0, 'ALL', NULL, NULL, 'ACTIVE', SYSDATETIME(), SYSDATETIME());
SET IDENTITY_INSERT seller.Coupon OFF;
```

## 修改重點整理

- 將資料庫物件命名調整為 SQL Server Schema 架構：schema 使用小寫模組名，資料表使用 PascalCase，欄位維持小寫 snake_case。
- 將識別碼統一改為 `int`，符合 MSSQL 與後端 JPA 的使用習慣。
- 將 `catalog.ProductSku` 使用通用規格欄位 `spec1_name`、`spec1_value`、`spec2_name`、`spec2_value`。
- D 模組改為保留必要快照欄位，`sales.Orders` 保存收件資料快照，`sales.OrderItem` 保存商品、SKU 與主圖快照。
- 將原本重複型態的訊息結構整理成 `msg.MsgTemplate`、`msg.MsgSample`、`msg.Msg` 與 `msg.MsgRecipient`，讓訊息樣本、模板與實際收件狀態分離。
- 新增評價模組 `review.ProductRecord`、`review.MemberRecord`，歸類到 F 組員；`review` 作為正式 SQL Server Schema，不使用 `public` 或縮寫 `rev`。
- 新增 `member.MemberOAuthAccount` 保存 Google 等第三方登入帳號對照。
- 新增 `ai.AiConversation` 保存雲端對話 ID、本地會員與聊天文字檔路徑的對照，暫定由全體組員共同負責。
- `seller.Seller` 商家資料歸類到 E 賣家中心後台模組，B 商品目錄透過 `catalog.Product.seller_id` 串接賣家。
- 新增 `seller.SellerAiSalesAnalysis` 保存賣家 AI 銷售分析結果，歸類到 E 賣家中心模組，並透過 `seller_id` 串接 `seller.Seller`。
- 客服模組歸類到 F 組員，並統一使用 `service` Schema 管理 `service.Role`、`service.Topic`、`service.Subtheme`、`service.Demand`、`service.Reply`。
- 補上建表順序，讓建立資料表時可以依順序執行，避免外鍵錯誤。
- 補上 seed data，方便開發測試與驗證資料關聯。

## 驗收標準

完成 0801 ERD 修改後，建議用以下文字標準檢查：

- DBML 貼到 dbdiagram.io 後，應產生 35 張資料表。
- DBML 不額外設定 ERD 表頭顏色，表頭維持 dbdiagram 預設樣式。
- Review 與 Service 不再作為獨立組員列入責任分配，應歸類到 F 組員。
- E 賣家中心模組包含 `seller.Seller`、`seller.Coupon`、`seller.MemberCoupon`、`seller.SellerAiSalesAnalysis`；其中 `seller.Coupon` 與 `seller.SellerAiSalesAnalysis` 直接以 `seller_id` 串接 `seller.Seller`，`seller.MemberCoupon` 透過 `seller.Coupon` 串接賣家。
- AI 模組 `ai.AiConversation` 暫定由全體組員共同負責。
- 資料庫命名應使用 SQL Server Schema 架構；不得使用 PostgreSQL 預設 Schema `public`。
- Schema 使用小寫模組名，資料表使用 PascalCase，例如 member.Member、member.Address、
eview.ProductRecord。
- 不應保留舊訊息表名 `notification_template`、`notification`；訊息相關表應為 `msg.MsgTemplate`、`msg.MsgSample`、`msg.Msg`、`msg.MsgRecipient`。
- 訊息型別欄位統一使用 `msg_type`，不再混用 `mesg_type` 或 `message_type`。
- `member.Member` 與 `cart.Cart` 應為一對一，`cart.Cart.member_id` 必須唯一。
- `cart.CartItem` 只保留 `sku_id`，不保留 `product_id`，並有 `UNIQUE(cart_id, sku_id)`。
- `catalog.Category.category_name` 必須為 `NOT NULL + UNIQUE`。
- `catalog.Subcategory` 必須有 `UNIQUE(category_id, subcategory_name)`。
- `catalog.Product.seller_id` 必須為 `NOT NULL`。
- `sales.Orders` 應保留 `receiver_name`、`receiver_phone`、`shipping_postal_code`、`shipping_city`、`shipping_district`、`shipping_detail_address` 作為下單快照。
- `sales.OrderItem` 應保留 `product_name`、`sku_spec`、`product_image_url` 作為下單快照。
- `review` 作為正式 SQL Server Schema；正式表名為 `review.ProductRecord`、`review.MemberRecord`，不縮寫為 `rev`。
- MSSQL 建表腳本在空資料庫執行時，應可建立 9 個 Schema 與 35 張資料表。
- Seed Data 應在建表完成後執行，且至少能建立角色、會員、商家、分類、品牌、商品、SKU、付款方式與優惠券基本測試資料。

## 1NF / 2NF / 3NF 檢查結果

### 1NF（第一正規化）

- 已符合 1NF。
- 每一欄位都是單一原子值，不包含多值欄位或巢狀資料。
- `catalog.ProductSku` 的規格欄位已改成明確欄位，避免後續因新增規格而頻繁擴增欄位。

### 2NF（第二正規化）

- 已符合 2NF。
- 所有非主鍵欄位皆完全相依於整個主鍵。
- 例如 `member.MemberRole` 的 `assigned_at` 完全依賴 `(member_id, role_id)`，沒有部份相依。

### 3NF（第三正規化）

- 已大幅接近並符合 3NF 的核心精神。
- D 模組保留訂單快照欄位，確保商品或地址資料異動後，歷史訂單仍能顯示下單當下內容。
- 目前資料關聯皆透過外鍵連結，資料只保留本表應有的屬性。
- `msg.MsgTemplate`、`msg.MsgSample`、`msg.Msg` 與 `msg.MsgRecipient` 取代原本重複型別訊息表，避免相同資訊分散在多張表，並保留會員收件狀態。
- `review.ProductRecord` 以 `order_item_id` 對應商品評價來源，避免重複保存可由訂單明細取得的商品資料。
- `review.MemberRecord` 以 `order_id`、`reviewer_id`、`target_member_id` 描述會員互評來源與對象。

### 最終判斷

- 1NF：符合
- 2NF：符合
- 3NF：符合 3NF 核心設計原則

## MSSQL 建表腳本（完整版）

這一段是可以直接貼到 MSSQL（Microsoft SQL Server）執行的 T-SQL 建表腳本，建議使用 SSMS 或 Azure Data Studio，先選定一個空資料庫後再執行。若資料庫已存在同名資料表，`CREATE TABLE` 會失敗，需要先確認是否要保留舊資料或另外建立新資料庫。`GO` 是 SSMS、sqlcmd、Azure Data Studio 支援的批次分隔語法；若改用 JDBC migration 工具，需將 `GO` 拆成不同批次處理。

```sql
-- 區塊 3：MSSQL 建表腳本（完整版）
-- 用途：建立 MSSQL 資料表、Schema、PK、FK、UNIQUE、CHECK、DEFAULT 與索引。
-- 建議：在空資料庫執行；若已有同名資料表，CREATE TABLE 會失敗。

CREATE SCHEMA member;
GO
CREATE SCHEMA seller;
GO
CREATE SCHEMA catalog;
GO
CREATE SCHEMA cart;
GO
CREATE SCHEMA sales;
GO
CREATE SCHEMA review;
GO
CREATE SCHEMA msg;
GO
CREATE SCHEMA ai;
GO
CREATE SCHEMA service;
GO
CREATE TABLE member.Member (
    member_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_member PRIMARY KEY,
    email varchar(100) NOT NULL CONSTRAINT uq_member_email UNIQUE,
    password_hash varchar(255) NOT NULL,
    last_name nvarchar(50) NOT NULL,
    first_name nvarchar(50) NOT NULL,
    birth_date date NULL,
    phone varchar(20) NULL,
    status varchar(20) NOT NULL CONSTRAINT df_member_status DEFAULT 'ACTIVE',
    created_at datetime2 NOT NULL CONSTRAINT df_member_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_member_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE member.Address (
    address_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_address PRIMARY KEY,
    member_id int NOT NULL CONSTRAINT fk_address_member FOREIGN KEY REFERENCES member.Member(member_id),
    receiver_name nvarchar(100) NOT NULL,
    receiver_phone varchar(20) NOT NULL,
    postal_code varchar(10) NULL,
    city nvarchar(50) NOT NULL,
    district nvarchar(50) NOT NULL,
    detail_address nvarchar(255) NOT NULL,
    is_default bit NOT NULL CONSTRAINT df_address_is_default DEFAULT 0,
    created_at datetime2 NOT NULL CONSTRAINT df_address_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_address_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE member.Role (
    role_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_role PRIMARY KEY,
    role_name varchar(50) NOT NULL CONSTRAINT uq_role_role_name UNIQUE,
    description nvarchar(100) NULL
);

CREATE TABLE member.MemberRole (
    member_id int NOT NULL CONSTRAINT fk_member_role_member FOREIGN KEY REFERENCES member.Member(member_id),
    role_id int NOT NULL CONSTRAINT fk_member_role_role FOREIGN KEY REFERENCES member.Role(role_id),
    assigned_at datetime2 NOT NULL CONSTRAINT df_member_role_assigned_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_member_role PRIMARY KEY (member_id, role_id)
);

CREATE TABLE member.MemberOAuthAccount (
    oauth_account_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_member_oauth_account PRIMARY KEY,
    member_id int NOT NULL CONSTRAINT fk_member_oauth_account_member FOREIGN KEY REFERENCES member.Member(member_id),
    provider varchar(30) NOT NULL,
    provider_user_id varchar(255) NOT NULL,
    provider_email varchar(255) NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_member_oauth_account_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_member_oauth_account_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_member_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_member_oauth_member_provider UNIQUE (member_id, provider)
);

CREATE TABLE seller.Seller (
    seller_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_seller PRIMARY KEY,
    member_id int NOT NULL CONSTRAINT uq_seller_member UNIQUE CONSTRAINT fk_seller_member FOREIGN KEY REFERENCES member.Member(member_id),
    store_name varchar(100) NOT NULL,
    store_description varchar(500) NULL,
    store_logo_url varchar(255) NULL,
    status varchar(30) NOT NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_seller_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_seller_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE catalog.Category (
    category_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_category PRIMARY KEY,
    category_name nvarchar(100) NOT NULL CONSTRAINT uq_category_name UNIQUE
);

CREATE TABLE catalog.Subcategory (
    subcategory_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_subcategory PRIMARY KEY,
    category_id int NOT NULL CONSTRAINT fk_subcategory_category FOREIGN KEY REFERENCES catalog.Category(category_id),
    subcategory_name nvarchar(100) NOT NULL,
    CONSTRAINT uq_subcategory_category_name UNIQUE (category_id, subcategory_name)
);

CREATE TABLE catalog.Brand (
    brand_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_brand PRIMARY KEY,
    brand_name nvarchar(100) NOT NULL
);

CREATE TABLE catalog.Product (
    product_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_product PRIMARY KEY,
    seller_id int NOT NULL CONSTRAINT fk_product_seller FOREIGN KEY REFERENCES seller.Seller(seller_id),
    subcategory_id int NOT NULL CONSTRAINT fk_product_subcategory FOREIGN KEY REFERENCES catalog.Subcategory(subcategory_id),
    brand_id int NOT NULL CONSTRAINT fk_product_brand FOREIGN KEY REFERENCES catalog.Brand(brand_id),
    product_name nvarchar(50) NOT NULL,
    description nvarchar(3000) NULL,
    base_price decimal(10,2) NOT NULL,
    status bit NOT NULL CONSTRAINT df_product_status DEFAULT 1,
    view_count int NOT NULL CONSTRAINT df_product_view_count DEFAULT 0,
    sold_count int NOT NULL CONSTRAINT df_product_sold_count DEFAULT 0,
    created_at datetime2 NOT NULL CONSTRAINT df_product_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_product_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE catalog.ProductSku (
    sku_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_product_sku PRIMARY KEY,
    product_id int NOT NULL CONSTRAINT fk_product_sku_product FOREIGN KEY REFERENCES catalog.Product(product_id),
    spec1_name nvarchar(30) NULL,
    spec1_value nvarchar(50) NULL,
    spec2_name nvarchar(30) NULL,
    spec2_value nvarchar(50) NULL,
    price decimal(10,2) NOT NULL,
    stock int NOT NULL CONSTRAINT ck_product_sku_stock CHECK (stock >= 0),
    status tinyint NOT NULL CONSTRAINT df_product_sku_status DEFAULT 1
);

CREATE TABLE catalog.ProductImage (
    image_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_product_image PRIMARY KEY,
    product_id int NOT NULL CONSTRAINT fk_product_image_product FOREIGN KEY REFERENCES catalog.Product(product_id),
    image_url nvarchar(255) NOT NULL,
    sort_order int NOT NULL,
    is_main bit NOT NULL CONSTRAINT df_product_image_is_main DEFAULT 0
);

CREATE TABLE cart.Cart (
    cart_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_cart PRIMARY KEY,
    member_id int NOT NULL CONSTRAINT uq_cart_member UNIQUE CONSTRAINT fk_cart_member FOREIGN KEY REFERENCES member.Member(member_id)
);

CREATE TABLE cart.CartItem (
    cart_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_cart_item PRIMARY KEY,
    cart_id int NOT NULL CONSTRAINT fk_cart_item_cart FOREIGN KEY REFERENCES cart.Cart(cart_id),
    sku_id int NOT NULL CONSTRAINT fk_cart_item_sku FOREIGN KEY REFERENCES catalog.ProductSku(sku_id),
    quantity int NOT NULL CONSTRAINT ck_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT uq_cart_item_cart_sku UNIQUE (cart_id, sku_id)
);

CREATE TABLE cart.Favorite (
    favorite_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_favorite PRIMARY KEY,
    member_id int NOT NULL CONSTRAINT fk_favorite_member FOREIGN KEY REFERENCES member.Member(member_id),
    product_id int NOT NULL CONSTRAINT fk_favorite_product FOREIGN KEY REFERENCES catalog.Product(product_id),
    CONSTRAINT uq_favorite_member_product UNIQUE (member_id, product_id)
);

CREATE TABLE sales.Orders (
    order_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_orders PRIMARY KEY,
    order_no varchar(30) NOT NULL CONSTRAINT uq_orders_order_no UNIQUE,
    buyer_id int NOT NULL CONSTRAINT fk_orders_buyer FOREIGN KEY REFERENCES member.Member(member_id),
    seller_id int NOT NULL CONSTRAINT fk_orders_seller FOREIGN KEY REFERENCES seller.Seller(seller_id),
    address_id int NULL CONSTRAINT fk_orders_address FOREIGN KEY REFERENCES member.Address(address_id),
    receiver_name nvarchar(100) NOT NULL,
    receiver_phone varchar(20) NOT NULL,
    shipping_postal_code varchar(10) NULL,
    shipping_city nvarchar(50) NOT NULL,
    shipping_district nvarchar(50) NOT NULL,
    shipping_detail_address nvarchar(255) NOT NULL,
    status varchar(30) NOT NULL CONSTRAINT df_orders_status DEFAULT 'PENDING_PAYMENT',
    subtotal_amount decimal(12,2) NOT NULL,
    shipping_fee decimal(12,2) NOT NULL CONSTRAINT df_orders_shipping_fee DEFAULT 0,
    discount_amount decimal(12,2) NOT NULL CONSTRAINT df_orders_discount_amount DEFAULT 0,
    total_amount decimal(12,2) NOT NULL,
    buyer_remark nvarchar(500) NULL,
    cancel_reason nvarchar(500) NULL,
    cancelled_by varchar(20) NULL,
    cancelled_at datetime2 NULL,
    completed_at datetime2 NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_orders_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_orders_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE sales.OrderItem (
    order_item_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_order_item PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT fk_order_item_orders FOREIGN KEY REFERENCES sales.Orders(order_id),
    product_id int NOT NULL CONSTRAINT fk_order_item_product FOREIGN KEY REFERENCES catalog.Product(product_id),
    sku_id int NOT NULL CONSTRAINT fk_order_item_sku FOREIGN KEY REFERENCES catalog.ProductSku(sku_id),
    product_name nvarchar(100) NOT NULL,
    sku_spec nvarchar(200) NULL,
    product_image_url nvarchar(500) NULL,
    unit_price decimal(12,2) NOT NULL,
    quantity int NOT NULL CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    subtotal decimal(12,2) NOT NULL,
    is_reviewed bit NOT NULL CONSTRAINT df_order_item_is_reviewed DEFAULT 0,
    created_at datetime2 NOT NULL CONSTRAINT df_order_item_created_at DEFAULT SYSDATETIME()
);

CREATE TABLE sales.PaymentMethod (
    payment_method_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment_method PRIMARY KEY,
    method_code varchar(30) NOT NULL CONSTRAINT uq_payment_method_method_code UNIQUE,
    method_name nvarchar(50) NOT NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_payment_method_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_payment_method_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE sales.Payment (
    payment_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment PRIMARY KEY,
    payment_no varchar(40) NOT NULL CONSTRAINT uq_payment_payment_no UNIQUE,
    order_id int NOT NULL CONSTRAINT fk_payment_orders FOREIGN KEY REFERENCES sales.Orders(order_id),
    payment_method_id int NOT NULL CONSTRAINT fk_payment_payment_method FOREIGN KEY REFERENCES sales.PaymentMethod(payment_method_id),
    amount decimal(12,2) NOT NULL,
    status varchar(20) NOT NULL CONSTRAINT df_payment_status DEFAULT 'PENDING',
    transaction_no varchar(100) NULL,
    failure_reason nvarchar(255) NULL,
    paid_at datetime2 NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_payment_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_payment_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE sales.Shipment (
    shipment_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_shipment PRIMARY KEY,
    order_id int NOT NULL CONSTRAINT uq_shipment_order UNIQUE CONSTRAINT fk_shipment_orders FOREIGN KEY REFERENCES sales.Orders(order_id),
    carrier_name nvarchar(100) NULL,
    tracking_no varchar(100) NULL,
    status varchar(30) NOT NULL CONSTRAINT df_shipment_status DEFAULT 'PREPARING',
    shipped_at datetime2 NULL,
    available_pickup_at datetime2 NULL,
    delivered_at datetime2 NULL,
    delivery_photo_url nvarchar(500) NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_shipment_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_shipment_updated_at DEFAULT SYSDATETIME()
);

CREATE TABLE review.ProductRecord (
    product_record_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_product_record PRIMARY KEY,
    order_item_id int NOT NULL,
    reviewer_id int NOT NULL,
    rating tinyint NOT NULL CONSTRAINT ck_product_record_rating CHECK (rating BETWEEN 1 AND 5),
    content nvarchar(1000) NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_product_record_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_product_record_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_product_record_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_product_record_order_item FOREIGN KEY (order_item_id) REFERENCES sales.OrderItem(order_item_id),
    CONSTRAINT fk_product_record_reviewer FOREIGN KEY (reviewer_id) REFERENCES member.Member(member_id)
);

CREATE TABLE review.MemberRecord (
    member_record_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_member_record PRIMARY KEY,
    order_id int NOT NULL,
    reviewer_id int NOT NULL,
    target_member_id int NOT NULL,
    rating tinyint NOT NULL CONSTRAINT ck_member_record_rating CHECK (rating BETWEEN 1 AND 5),
    content nvarchar(1000) NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_member_record_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_member_record_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_member_record_order_members UNIQUE (order_id, reviewer_id, target_member_id),
    CONSTRAINT fk_member_record_orders FOREIGN KEY (order_id) REFERENCES sales.Orders(order_id),
    CONSTRAINT fk_member_record_reviewer FOREIGN KEY (reviewer_id) REFERENCES member.Member(member_id),
    CONSTRAINT fk_member_record_target_member FOREIGN KEY (target_member_id) REFERENCES member.Member(member_id)
);

CREATE TABLE seller.Coupon (
    coupon_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_coupon PRIMARY KEY,
    seller_id int NOT NULL CONSTRAINT fk_coupon_seller FOREIGN KEY REFERENCES seller.Seller(seller_id),
    coupon_code varchar(100) NOT NULL,
    coupon_name varchar(100) NOT NULL,
    discount_type varchar(30) NOT NULL,
    discount_value decimal(18,2) NOT NULL,
    min_purchase_amount decimal(18,2) NULL,
    start_at datetime2 NOT NULL,
    end_at datetime2 NOT NULL,
    limit_count int NULL,
    used_count int NOT NULL CONSTRAINT df_coupon_used_count DEFAULT 0,
    scope_type varchar(30) NOT NULL,
    category_id int NULL CONSTRAINT fk_coupon_category FOREIGN KEY REFERENCES catalog.Category(category_id),
    product_id int NULL CONSTRAINT fk_coupon_product FOREIGN KEY REFERENCES catalog.Product(product_id),
    status varchar(30) NOT NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_coupon_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_coupon_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_coupon_seller_code UNIQUE (seller_id, coupon_code),
    CONSTRAINT ck_coupon_valid_time CHECK (end_at > start_at),
    CONSTRAINT ck_coupon_discount_type CHECK (discount_type IN ('PERCENT', 'AMOUNT')),
    CONSTRAINT ck_coupon_scope_type CHECK (scope_type IN ('ALL', 'CATEGORY', 'PRODUCT')),
    CONSTRAINT ck_coupon_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_coupon_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupon_limit_count CHECK (limit_count IS NULL OR limit_count > 0)
);

CREATE TABLE seller.MemberCoupon (
    member_coupon_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_member_coupon PRIMARY KEY,
    coupon_id int NOT NULL CONSTRAINT fk_member_coupon_coupon FOREIGN KEY REFERENCES seller.Coupon(coupon_id),
    member_id int NOT NULL CONSTRAINT fk_member_coupon_member FOREIGN KEY REFERENCES member.Member(member_id),
    is_used bit NOT NULL CONSTRAINT df_member_coupon_is_used DEFAULT 0,
    used_at datetime2 NULL,
    received_at datetime2 NOT NULL CONSTRAINT df_member_coupon_received_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_member_coupon UNIQUE (coupon_id, member_id)
);

CREATE TABLE seller.SellerAiSalesAnalysis (
    analysis_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_seller_ai_sales_analysis PRIMARY KEY,
    seller_id int NOT NULL CONSTRAINT fk_seller_ai_sales_analysis_seller FOREIGN KEY REFERENCES seller.Seller(seller_id),
    analysis_period_start date NOT NULL,
    analysis_period_end date NOT NULL,
    revenue_amount decimal(12,2) NOT NULL CONSTRAINT df_seller_ai_sales_analysis_revenue DEFAULT 0,
    order_count int NOT NULL CONSTRAINT df_seller_ai_sales_analysis_order_count DEFAULT 0,
    product_count int NOT NULL CONSTRAINT df_seller_ai_sales_analysis_product_count DEFAULT 0,
    used_coupon_count int NOT NULL CONSTRAINT df_seller_ai_sales_analysis_used_coupon_count DEFAULT 0,
    top_product_summary nvarchar(500) NULL,
    coupon_summary nvarchar(500) NULL,
    risk_summary nvarchar(500) NULL,
    ai_summary nvarchar(1000) NOT NULL,
    ai_recommendation nvarchar(1000) NULL,
    model_name varchar(100) NULL,
    generated_at datetime2 NOT NULL CONSTRAINT df_seller_ai_sales_analysis_generated_at DEFAULT SYSDATETIME(),
    created_at datetime2 NOT NULL CONSTRAINT df_seller_ai_sales_analysis_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT ck_seller_ai_sales_analysis_period CHECK (analysis_period_end >= analysis_period_start),
    CONSTRAINT ck_seller_ai_sales_analysis_revenue CHECK (revenue_amount >= 0),
    CONSTRAINT ck_seller_ai_sales_analysis_order_count CHECK (order_count >= 0),
    CONSTRAINT ck_seller_ai_sales_analysis_product_count CHECK (product_count >= 0),
    CONSTRAINT ck_seller_ai_sales_analysis_used_coupon_count CHECK (used_coupon_count >= 0)
);

CREATE TABLE msg.MsgTemplate (
    template_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_msg_template PRIMARY KEY,
    role_type char(2) NOT NULL,
    msg_type varchar(50) NOT NULL,
    title nvarchar(50) NOT NULL,
    content nvarchar(500) NOT NULL,
    coupon_id int NULL CONSTRAINT fk_msg_template_coupon FOREIGN KEY REFERENCES seller.Coupon(coupon_id)
);

CREATE TABLE msg.MsgSample (
    sample_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_msg_sample PRIMARY KEY,
    role_type char(2) NOT NULL,
    msg_type varchar(50) NOT NULL,
    sample_subject nvarchar(500) NOT NULL,
    sample_content nvarchar(500) NOT NULL,
    sample_at datetime2 NOT NULL CONSTRAINT df_msg_sample_sample_at DEFAULT SYSDATETIME()
);

CREATE TABLE msg.Msg (
    msg_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_msg PRIMARY KEY,
    template_id int NULL CONSTRAINT fk_msg_template FOREIGN KEY REFERENCES msg.MsgTemplate(template_id),
    sender_id int NOT NULL CONSTRAINT fk_msg_sender FOREIGN KEY REFERENCES member.Member(member_id),
    title nvarchar(200) NOT NULL,
    content nvarchar(5000) NOT NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_msg_created_at DEFAULT SYSDATETIME()
);

CREATE TABLE msg.MsgRecipient (
    msg_recipient_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_msg_recipient PRIMARY KEY,
    msg_id int NOT NULL CONSTRAINT fk_msg_recipient_msg FOREIGN KEY REFERENCES msg.Msg(msg_id),
    member_id int NOT NULL CONSTRAINT fk_msg_recipient_member FOREIGN KEY REFERENCES member.Member(member_id),
    is_read bit NOT NULL CONSTRAINT df_msg_recipient_is_read DEFAULT 0,
    read_at datetime2 NULL,
    delivered_at datetime2 NOT NULL CONSTRAINT df_msg_recipient_delivered_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_msg_recipient UNIQUE (msg_id, member_id)
);

CREATE TABLE ai.AiConversation (
    conversation_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_ai_conversation PRIMARY KEY,
    cloud_conversation_id varchar(255) NOT NULL,
    member_id int NOT NULL CONSTRAINT fk_ai_conversation_member FOREIGN KEY REFERENCES member.Member(member_id),
    provider varchar(50) NOT NULL,
    log_file_path nvarchar(500) NOT NULL,
    created_at datetime2 NOT NULL CONSTRAINT df_ai_conversation_created_at DEFAULT SYSDATETIME(),
    updated_at datetime2 NOT NULL CONSTRAINT df_ai_conversation_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT uq_ai_conversation_cloud_id UNIQUE (provider, cloud_conversation_id)
);

CREATE TABLE service.Role (
    service_role_id int NOT NULL CONSTRAINT pk_service_role PRIMARY KEY,
    role_name varchar(50) NOT NULL CHECK (role_name IN ('customer', 'seller'))
);

CREATE TABLE service.Topic (
    topic_id char(1) NOT NULL CONSTRAINT pk_service_topic PRIMARY KEY,
    role_name varchar(50) NOT NULL CHECK (role_name IN ('customer', 'seller')),
    topic nvarchar(50) NULL,
    topic_enter nvarchar(100) NULL
);

CREATE TABLE service.Subtheme (
    subtheme_id int NOT NULL CONSTRAINT pk_service_subtheme PRIMARY KEY,
    role_name varchar(50) NOT NULL CHECK (role_name IN ('customer', 'seller')),
    topic nvarchar(50) NULL,
    subtheme nvarchar(50) NULL,
    subtheme_enter nvarchar(100) NULL
);

CREATE TABLE service.Demand (
    demand_id int NOT NULL CONSTRAINT pk_service_demand PRIMARY KEY,
    role_name varchar(50) NOT NULL CHECK (role_name IN ('customer', 'seller')),
    subtheme nvarchar(50) NULL,
    demand nvarchar(50) NULL,
    demand_enter nvarchar(100) NULL
);

CREATE TABLE service.Reply (
    sys_reply_id int NOT NULL CONSTRAINT pk_service_reply PRIMARY KEY,
    role_name varchar(50) NOT NULL CHECK (role_name IN ('customer', 'seller')),
    demand nvarchar(50) NULL,
    reply nvarchar(100) NULL,
    reply_enter nvarchar(100) NULL
);

CREATE INDEX ix_member_email ON member.Member(email);
CREATE INDEX ix_address_member_id ON member.Address(member_id);
CREATE INDEX ix_subcategory_category_id ON catalog.Subcategory(category_id);
CREATE INDEX ix_product_seller_id ON catalog.Product(seller_id);
CREATE INDEX ix_product_subcategory_id ON catalog.Product(subcategory_id);
CREATE INDEX ix_product_brand_id ON catalog.Product(brand_id);
CREATE INDEX ix_product_sku_product_id ON catalog.ProductSku(product_id);
CREATE INDEX ix_product_image_product_id ON catalog.ProductImage(product_id);
CREATE INDEX ix_cart_item_cart_id ON cart.CartItem(cart_id);
CREATE INDEX ix_cart_item_sku_id ON cart.CartItem(sku_id);
CREATE INDEX ix_favorite_member_id ON cart.Favorite(member_id);
CREATE INDEX ix_favorite_product_id ON cart.Favorite(product_id);
CREATE INDEX ix_orders_buyer_id ON sales.Orders(buyer_id);
CREATE INDEX ix_orders_seller_id ON sales.Orders(seller_id);
CREATE INDEX ix_orders_status ON sales.Orders(status);
CREATE INDEX ix_orders_buyer_created_at ON sales.Orders(buyer_id, created_at);
CREATE INDEX ix_orders_seller_status_created_at ON sales.Orders(seller_id, status, created_at);
CREATE INDEX ix_order_item_order_id ON sales.OrderItem(order_id);
CREATE INDEX ix_order_item_product_id ON sales.OrderItem(product_id);
CREATE INDEX ix_order_item_sku_id ON sales.OrderItem(sku_id);
CREATE INDEX ix_order_item_is_reviewed ON sales.OrderItem(is_reviewed);
CREATE INDEX ix_payment_order_id ON sales.Payment(order_id);
CREATE INDEX ix_payment_payment_method_id ON sales.Payment(payment_method_id);
CREATE INDEX ix_payment_status ON sales.Payment(status);
CREATE INDEX ix_payment_order_status ON sales.Payment(order_id, status);
CREATE INDEX ix_shipment_status ON sales.Shipment(status);
CREATE INDEX ix_shipment_tracking_no ON sales.Shipment(tracking_no);
CREATE INDEX ix_product_record_reviewer_id ON review.ProductRecord(reviewer_id);
CREATE INDEX ix_member_record_order_id ON review.MemberRecord(order_id);
CREATE INDEX ix_member_record_reviewer_id ON review.MemberRecord(reviewer_id);
CREATE INDEX ix_member_record_target_member_id ON review.MemberRecord(target_member_id);
CREATE INDEX ix_coupon_seller_id ON seller.Coupon(seller_id);
CREATE INDEX ix_coupon_category_id ON seller.Coupon(category_id);
CREATE INDEX ix_coupon_product_id ON seller.Coupon(product_id);
CREATE INDEX ix_coupon_status ON seller.Coupon(status);
CREATE INDEX ix_member_coupon_coupon_id ON seller.MemberCoupon(coupon_id);
CREATE INDEX ix_member_coupon_member_id ON seller.MemberCoupon(member_id);
CREATE INDEX ix_msg_template_coupon_id ON msg.MsgTemplate(coupon_id);
CREATE INDEX ix_msg_member_id ON msg.Msg(sender_id);
CREATE INDEX ix_msg_template_id ON msg.Msg(template_id);
CREATE INDEX ix_msg_recipient_msg_id ON msg.MsgRecipient(msg_id);
CREATE INDEX ix_msg_recipient_member_id ON msg.MsgRecipient(member_id);
CREATE INDEX ix_member_oauth_account_member_id ON member.MemberOAuthAccount(member_id);
CREATE INDEX ix_ai_conversation_member_id ON ai.AiConversation(member_id);
CREATE INDEX ix_seller_ai_sales_analysis_seller_generated ON seller.SellerAiSalesAnalysis(seller_id, generated_at DESC);
CREATE INDEX ix_seller_ai_sales_analysis_period ON seller.SellerAiSalesAnalysis(seller_id, analysis_period_start, analysis_period_end);
```

## 表總數統計

目前這份資料模型總共有 35 張表。A-F 組員負責 34 張，AI 模組 1 張暫定由全體組員共同負責。

| 模組 / 組員                       | 資料表                                                                                                                                                                          | 小計 |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---- |
| A：會員與帳號模組                 | `member.Member`, `member.Address`, `member.Role`, `member.MemberRole`, `member.MemberOAuthAccount`                                                                                                              | 5    |
| B：商品目錄模組                   | `catalog.Category`, `catalog.Subcategory`, `catalog.Brand`, `catalog.Product`, `catalog.ProductSku`, `catalog.ProductImage`                                                                                                   | 6    |
| C：購物車與收藏模組               | `cart.Cart`, `cart.CartItem`, `cart.Favorite`                                                                                                                                                 | 3    |
| D：訂單、付款與物流模組           | `sales.Orders`, `sales.OrderItem`, `sales.PaymentMethod`, `sales.Payment`, `sales.Shipment`                                                                                                                 | 5    |
| E：賣家中心模組                   | `seller.Seller`, `seller.Coupon`, `seller.MemberCoupon`, `seller.SellerAiSalesAnalysis`                                                                                                                  | 4    |
| F：通知、評價與客服模組           | `msg.MsgTemplate`, `msg.MsgSample`, `msg.Msg`, `msg.MsgRecipient`, `review.ProductRecord`, `review.MemberRecord`, `service.Role`, `service.Topic`, `service.Subtheme`, `service.Demand`, `service.Reply` | 11   |
| 全體組員（暫定）：AI 模組         | `ai.AiConversation`                                                                                                                                                               | 1    |
| 總計                              | A-F 小計 34 張，AI 全體暫定負責 1 張                                                                                                                                            | 35   |
