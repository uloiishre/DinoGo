package com.dinogo.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    public Optional<Member> findByEmail(String email);

    public Optional<Member> findByEmailIgnoreCase(String email);

    public boolean existsByEmail(String email);

    public boolean existsByEmailIgnoreCase(String email);

    /**
     * Atomically updates a password only while the reset token's member state is still current.
     * A return value of zero means the token was already consumed or is no longer valid.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member member
               set member.passwordHash = :passwordHash,
                   member.authVersion = member.authVersion + 1,
                   member.updatedAt = CURRENT_TIMESTAMP
             where member.memberId = :memberId
               and member.authVersion = :authVersion
               and member.status = 'ACTIVE'
               and lower(member.email) = lower(:email)
            """)
    int resetPasswordIfTokenIsValid(
            @Param("memberId") Integer memberId,
            @Param("email") String email,
            @Param("authVersion") int authVersion,
            @Param("passwordHash") String passwordHash);

    @EntityGraph(attributePaths = { "memberRoles", "memberRoles.role" })
    public List<Member> findAllByStatusIgnoreCase(String status);
}
