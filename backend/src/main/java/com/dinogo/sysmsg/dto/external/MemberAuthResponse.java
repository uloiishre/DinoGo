package com.dinogo.sysmsg.dto.external;

/**External跨模組 API 的資料邊界
 * ============================================================
 * Member 模組 API 回傳
 * ============================================================
 *
 * 假設 Member 模組 API：
 *
 * GET /api/member/auth/validate
 *
 * 實際 port 由 member 模組決定。
 *
 * 例如：
 *
 * http://localhost:8081
 *
 * ============================================================
 *
 * sysmsg 使用目的：
 *
 * 1. 確認登入會員存在
 * 2. 取得 member_id
 * 3. 必要時確認 token / login 身分
 *
 * 注意：
 *
 * sysmsg 不重新實作完整登入系統。
 */
public class MemberAuthResponse {

    private Integer memberId;

    private Integer sellerId;

    private boolean authenticated;

    private String email;

    private String role;

    /** 會員中心「訂單訊息」電子郵件通知偏好。 */
    private Boolean emailOrderNotifications;

    /** 會員中心「行銷訊息」電子郵件通知偏好。 */
    private Boolean emailMarketingNotifications;


    public MemberAuthResponse() {
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEmailOrderNotifications() { return emailOrderNotifications; }
    public void setEmailOrderNotifications(Boolean value) { this.emailOrderNotifications = value; }
    public Boolean getEmailMarketingNotifications() { return emailMarketingNotifications; }
    public void setEmailMarketingNotifications(Boolean value) { this.emailMarketingNotifications = value; }
}
