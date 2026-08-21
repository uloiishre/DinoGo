package com.dinogo.seller.repository;

import java.util.Collection;
import java.util.List;
import com.dinogo.seller.entity.Seller;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
    Optional<Seller> findBySellerId(Integer sellerId);

    Optional<Seller> findByMember_MemberId(Integer memberId);

    @EntityGraph(attributePaths = "member")
    List<Seller> findByMember_MemberIdIn(Collection<Integer> memberIds);

    boolean existsBySellerId(Integer sellerId);

    boolean existsBySellerIdAndStatus(Integer sellerId, String status);

}
