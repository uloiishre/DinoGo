package com.dinogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.MemberRole;
import com.dinogo.entity.MemberRoleId;

public interface MemberRoleRepository extends JpaRepository<MemberRole, MemberRoleId> {

    public List<MemberRole> findByMemberMemberId(Integer memberId);

    public boolean existsByMemberMemberIdAndRoleRoleId(Integer memberId, Integer roleId);
}
