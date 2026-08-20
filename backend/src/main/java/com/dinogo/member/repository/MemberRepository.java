package com.dinogo.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    public Optional<Member> findByEmail(String email);

    public Optional<Member> findByEmailIgnoreCase(String email);

    public boolean existsByEmail(String email);

    public boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = { "memberRoles", "memberRoles.role" })
    public List<Member> findAllByStatusIgnoreCase(String status);
}
