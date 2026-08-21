package com.dinogo.seller.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.seller.entity.Seller;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
    Optional<Seller> findBySellerId(Integer sellerId);

    Optional<Seller> findByMember_MemberId(Integer memberId);

    @EntityGraph(attributePaths = "member")
    List<Seller> findByMember_MemberIdIn(Collection<Integer> memberIds);

    boolean existsBySellerId(Integer sellerId);

    boolean existsBySellerIdAndStatus(Integer sellerId, String status);

    boolean existsByMember_MemberId(Integer memberId);

    List<Seller> findByStoreNameContainingAndStatus(String keyword, String status);
}