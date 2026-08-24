package com.dinogo.sysmsg.dto.external;

import java.util.List;

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

    /** Member 模組依 memberId 提供的登入 Email；不是訂閱偏好欄位。 */
    private String email;

    private String role;

    /** 角色 ID 以 member.MemberRole 關聯 member.Role 的結果為準；3 為管理員。 */
    private List<Integer> roleIds = List.of();

    /** 訂單偏好三態：true=寄送、false=不寄送、null=無法驗證且預設寄送。 */
    private Boolean emailOrderNotifications; //假設Client-sysmsg：待 member 模組提供正式欄位。

    /** 行銷偏好三態：true=寄送、false/null=不寄送。 */
    private Boolean emailMarketingNotifications; //假設Client-sysmsg：待 member 模組提供正式欄位。


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

    public List<Integer> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Integer> roleIds) {
        this.roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }

    public Boolean getEmailOrderNotifications() { return emailOrderNotifications; }
    public void setEmailOrderNotifications(Boolean value) { this.emailOrderNotifications = value; }
    public Boolean getEmailMarketingNotifications() { return emailMarketingNotifications; }
    public void setEmailMarketingNotifications(Boolean value) { this.emailMarketingNotifications = value; }
}
