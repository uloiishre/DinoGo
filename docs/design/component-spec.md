# Component Specification

本階段只規劃必要的 layout shell 與導航元件；細部會員、商品、訂單元件留到頁面需求明確後再建立。

| Component | Category | 用途 | 全站 | Vue 檔案位置 | 建議分支 | 狀態規格 |
|---|---|---|---|---|---|---|
| `UtilityBar.vue` | Layout | 頂部客服、訊息、商家中心、語言 | Yes | `frontend/src/components/layout/UtilityBar.vue` | `feature/layout-shell` | default、hover、focus、disabled |
| `AppHeader.vue` | Layout | Logo、搜尋、收藏、通知、購物車、會員中心 | Yes | `frontend/src/components/layout/AppHeader.vue` | `feature/layout-shell` | default、hover、active、focus、badge |
| `SearchBar.vue` | Layout | 商品搜尋、分類入口、搜尋提交 | Yes | `frontend/src/components/layout/SearchBar.vue` | `feature/layout-shell` | default、focus、disabled、loading、error |
| `PrimaryNav.vue` | Layout | 全站分類、新品、熱門、商家、優惠 | Yes | `frontend/src/components/layout/PrimaryNav.vue` | `feature/layout-shell` | default、hover、active、focus、selected、dropdown |
| `AppFooter.vue` | Layout | 平台資訊、協助、客服、法律、社群 | Yes | `frontend/src/components/layout/AppFooter.vue` | `feature/layout-shell` | default、hover、focus、mobile accordion |
| `DefaultStorefrontLayout.vue` | Layout | 一般商城 shell | Shared | `frontend/src/layouts/DefaultStorefrontLayout.vue` | `feature/layout-shell` | loading、error、empty slot |
| `AuthLayout.vue` | Layout | 登入、註冊、忘記密碼 shell | Shared | `frontend/src/layouts/AuthLayout.vue` | `feature/layout-shell` | default、error |
| `MemberLayout.vue` | Layout | 會員中心 shell，組合 MemberNav | Shared | `frontend/src/layouts/MemberLayout.vue` | `feature/member-layout` | default、loading、error |
| `SellerLayout.vue` | Layout | 商家中心 shell，組合 SellerNav | Shared | `frontend/src/layouts/SellerLayout.vue` | `feature/seller-layout` | default、loading、error |
| `MemberNav.vue` | Member | 會員中心導覽與 active route | Shared | `frontend/src/components/member/MemberNav.vue` | `feature/member-layout` | default、hover、active、focus、selected、dropdown |
| `SellerNav.vue` | Seller | 商家中心導覽與 mobile offcanvas | Shared | `frontend/src/components/seller/SellerNav.vue` | `feature/seller-layout` | default、hover、active、focus、selected、disabled |

## 邊界規則

- Header/Footer/Layout 只由 layout shell 組合，view 不自行重複撰寫。
- `MemberNav` 只在 `MemberLayout` 使用；`SellerNav` 只在 `SellerLayout` 使用。
- `SearchBar` 屬 `AppHeader` 的可重用子元件，不在每個頁面另寫搜尋列。
- 連續出現兩次以上或跨頁共用才抽 component；單頁小區塊先保留在 view。
- `MemberSummaryCards`、`ProductCard`、`OrderCard` 等元件暫不建立。
