package com.dinogo.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.MemberRoleI
import com.dinogo.member.entity.MemberRoleId;Role;

public interface MemberRoleRepository extends JpaRepository<MemberRole, MemberRoleId> {

    public List<MemberRole> findByMemberMemberId(Integer memberId);

    public boolean existsByMemberMemberIdAndRoleRoleId(Integer memberId, Integer roleId);
}
