package com.dinogo.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    public Optional<Member> findByEmail(String email);

    public boolean existsByEmail(String email);
}
