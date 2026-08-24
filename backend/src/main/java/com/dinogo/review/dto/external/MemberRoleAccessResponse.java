package com.dinogo.review.dto.external;

import java.util.Set;

//review-start，總共1次修改，第1次//
/**
 * Review 向 member 模組索取的最小身分資料。
 *
 * <p>memberId 來源必須是 member.Member.memberID；roleIds 必須由該 memberId
 * 關聯 member.MemberRole，再取得 member.Role.role_id。Review 不接收前端自行
 * 宣告的角色，也不使用固定 memberId 或 sellerId 判斷管理員。</p>
 */
public record MemberRoleAccessResponse(
        Integer memberId,
        Set<Integer> roleIds) {

    public MemberRoleAccessResponse {
        roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
    }
}
//review-end，總共1次修改，第1次//
