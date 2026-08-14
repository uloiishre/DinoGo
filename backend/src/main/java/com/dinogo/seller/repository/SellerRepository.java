package com.dinogo.seller.repository;

import com.dinogo.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
    Optional<Seller> findBySellerId(Integer sellerId);

    Optional<Seller> findByMember_MemberId(Integer memberId);

    boolean existsBySellerId(Integer sellerId);

    boolean existsBySellerIdAndStatus(Integer sellerId, String status);

}
